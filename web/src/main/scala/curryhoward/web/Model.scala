package curryhoward.web

import curryhoward.engine.ipl.*
import curryhoward.engine.ipl.nj.*
import curryhoward.engine.ipl.nj.Partial.*

enum Screen:
  case Home, Setup, Play

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
    selected: Option[Path]
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

object Model:

  val empty: Model = Model(Screen.Home, "", None, None, None)

  def start(goal: Goal): Model =
    Model(Screen.Play, "", Some(goal), Some(GameTree.start(goal.formula)), None)

  extension (m: Model)

    def play(moveIndex: Int): Model =
      (m.tree, m.selectedHole) match
        case (Some(tree), Some((path, _))) =>
          // Drop the selection: the hole just filled is gone, and the next
          // render picks the first open one.
          m.copy(tree = Some(tree.play(path, moveIndex)), selected = None)
        case _ => m

    def select(path: Path): Model = m.copy(selected = Some(path))

    def restart: Model = m.goal.fold(m)(g => Model.start(g))
