package curryhoward.web

import curryhoward.engine.ipl.*
import curryhoward.engine.ipl.Sequent.Prem
import curryhoward.engine.ipl.nj.*
import curryhoward.engine.ipl.nj.Partial.*

enum Screen:
  case Home, Setup, Play

/** The two readings of one game (D12). Switching is a *view* change and must
  * never touch game state — the whole claim of the project is that these are
  * two notations for the same thing, so the position cannot depend on which one
  * is on screen.
  */
enum View:
  case Programmer, Logician

  def isLogician: Boolean = this == View.Logician

  /** The notation the formulas are printed in. */
  def show(f: Formula): String =
    if isLogician then Notation.logician(f) else Notation.programmer(f)

/** The one destructive action on the Play screen, waiting to be confirmed.
  *
  * The interaction spec allows exactly one confirmation dialog in the whole
  * application, and this is it: both ways of abandoning a search go through it,
  * and nothing else does.
  */
enum Confirm:
  /** Same goal, empty tree. */
  case Restart

  /** Back to Home, game gone. */
  case Quit

/** Everything the app knows.
  *
  * Note what is *not* here: the moves, the holes, the term, whether the game is
  * won. All of those are functions of the position, recomputed per render, so
  * there is one source of truth and no state to keep in step. That is the
  * handoff's own model — "derived per render" — and it is what makes the view a
  * rendering of the engine rather than a copy of it.
  */
final case class Model(
    screen: Screen,
    input: String,
    goal: Option[Goal],
    tree: Option[GameTree],
    /** Which hole the rules table refers to.
      *
      * One hole is selected at a time, as the interaction spec draws it. The
      * console offers every move at every hole instead, which is quicker to
      * play but is an artifact of a text interface, where selecting costs a
      * typed command rather than a click.
      */
    selected: Option[Path],
    /** Which cell of the rules table is unfolded, if any.
      *
      * A cell with several concrete instances does not list them: it shows a
      * count and opens in place when clicked, as the interaction spec has it.
      * View state, not game state — nothing here survives a move.
      */
    openCell: Option[String] = None,
    /** Whether the search-path panel is expanded. Collapsed by default, as the
      * interaction spec has it: the rules table needs the room.
      */
    treeOpen: Boolean = false,
    /** A pending confirmation, if the player asked to abandon the game. */
    confirm: Option[Confirm] = None,
    /** Which reading is on screen. Not part of the game (D12). */
    view: View = View.Programmer
):

  def position: Option[Partial] = tree.map(_.current.position)

  def holes: List[(Path, Sequent)] = position.map(_.holes).getOrElse(Nil)

  /** The selected hole, falling back to the first — a selection can be
    * invalidated by the very move it was used to make.
    */
  def selectedHole: Option[(Path, Sequent)] =
    val hs = holes
    selected.flatMap(p => hs.find(_._1 == p)).orElse(hs.headOption)

  def moves: List[NJ[Sequent]] =
    selectedHole.map((_, hole) => NJ.coalg(hole).toList).getOrElse(Nil)

  def parsed: Either[ParseError, Goal] = Parser.parseGoal(input)

  def won: Boolean = position.exists(_.status == Status.Won)

  /** A hole with no move at all closes this *branch* — never the game (§4.7,
    * interaction spec §6.2). The player is offered a way back, not an ending.
    */
  def deadEnd: Boolean = position.exists(_.status == Status.Dead)

  /** The negative ending (§4.7, D13): every play from the opening tried, none
    * of them a proof. Ends the game, as winning does.
    *
    * Cheap in the common case despite walking the tree: the check gives up at
    * the first move nobody has taken yet, which at the root is nearly always
    * immediate.
    */
  def refuted: Boolean = tree.exists(_.refuted)

  /** This *line* has been played out and leads nowhere — every continuation
    * from here has been tried and every one of them failed.
    *
    * Not an ending: the game is only lost when the *root* is exhausted. It is
    * the honest way to tell a player that a wrong turn was a wrong turn, and it
    * is the only way they can find that out before Phase 10's hints — a line
    * that cannot be finished looks exactly like an ordinary position until it
    * has been explored.
    */
  def lineExhausted: Boolean = tree.exists(t => t.exhausted(t.currentId))

  /** The position read as a derivation and the facts beside it (D25). */
  def forest: Option[Figure.Forest] = position.map(ToFigure.apply)

  /** Which hole is selected, as an index into the position's hole list — the
    * figure numbers its open leaves the same way, so the two views agree on
    * what is selected.
    */
  def selectedIndex: Option[Int] =
    for
      (path, _) <- selectedHole
      i = holes.indexWhere(_._1 == path)
      if i >= 0
    yield i

  /** Where "backtrack" goes: the nearest ancestor that still has a real choice
    * to make, falling back to the parent, and then to the root.
    *
    * A choice point is a node with more than one move available across its
    * holes — which is where a different decision could have been taken. Landing
    * on the immediate parent of a dead end would usually just re-offer the move
    * that failed.
    */
  def backtrackTarget: Option[GameTree.NodeId] =
    tree.flatMap { t =>
      val ancestors = t.path(t.currentId).dropRight(1).reverse
      ancestors
        .find(node => t.goTo(node.id).options.sizeIs > 1)
        .orElse(ancestors.headOption)
        .map(_.id)
    }

object Model:

  val empty: Model = Model(Screen.Home, "", None, None, None)

  def start(goal: Goal): Model =
    Model(Screen.Play, "", Some(goal), Some(GameTree.start(goal.formula)), None)

  /** Resume a saved game (D4/D5). The goal comes back from the tree itself, so
    * a save carries everything the Play screen needs.
    */
  def resume(tree: GameTree): Model =
    Model(Screen.Play, "", Some(Goal.from(tree.goal)), Some(tree), None)

  extension (m: Model)

    /** Take a move at the selected hole.
      *
      * `andThen` is what makes a forward use **one stroke**. The engine's
      * `let` leaves a hole for its value, so `val qr: Either[Q, R] = …` and
      * `= pqr._2` are two of its moves; but they are one move in §3.2 and one
      * cell in the table, so choosing `∧.E₂` there plays both. It names the
      * elimination to apply at the value hole: its rule, and the resource it
      * acts on — the pair that identifies it among the moves offered there.
      *
      * The tree still records two nodes. That is visible in Phase 5's search
      * path, and is the price of keeping this out of the engine (D24).
      */
    def play(moveIndex: Int, andThen: Option[(String, Prem)] = None): Model =
      (m.tree, m.selectedHole) match
        case (Some(tree), Some((path, _))) =>
          val played = tree.play(path, moveIndex)
          val done = andThen.fold(played) { (rule, on) =>
            // A `let` opens its value first, so the value hole is child 0.
            val value = path :+ 0
            played.current.position
              .movesAt(value)
              .zipWithIndex
              .collectFirst {
                case (move, i) if NJ.label(move) == rule && NJ.actsOn(move).contains(on) => i
              }
              .fold(played)(played.play(value, _))
          }
          // Drop the selection: the hole just filled is gone, and the next
          // render picks the first open one.
          m.copy(tree = Some(done), selected = None, openCell = None)
        case _ => m

    def select(path: Path): Model = m.copy(selected = Some(path), openCell = None)

    /** Unfold a cell, or fold the one already open. */
    def toggleCell(key: String): Model =
      m.copy(openCell = if m.openCell.contains(key) then None else Some(key))

    def restart: Model = m.goal.fold(m)(g => Model.start(g))

    /** Jump to any node explored so far — which *is* backtracking (interaction
      * spec §6). Nothing is undone: the branch left behind stays in the tree,
      * and stays reachable.
      */
    def jump(id: GameTree.NodeId): Model =
      m.copy(tree = m.tree.map(_.goTo(id)), selected = None, openCell = None)

    def backtrack: Model = m.backtrackTarget.fold(m)(m.jump)

    def toggleTree: Model = m.copy(treeOpen = !m.treeOpen)

    /** Switch reading. Note what this copies: the view and nothing else. */
    def toggleView: Model =
      m.copy(view = if m.view.isLogician then View.Programmer else View.Logician)

    /** Select a hole by its index in the position's hole list — what the
      * logician view has to hand when a leaf is clicked.
      */
    def selectAt(index: Int): Model =
      m.holes.lift(index).fold(m)((path, _) => m.select(path))

    def ask(what: Confirm): Model = m.copy(confirm = Some(what))

    def dismiss: Model = m.copy(confirm = None)
