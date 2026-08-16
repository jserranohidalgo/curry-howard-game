package curryhoward.repl

import scala.io.StdIn
import curryhoward.engine.ipl.*
import curryhoward.engine.ipl.nj.*
import curryhoward.engine.ipl.nj.Partial.*
import curryhoward.engine.ipl.ljt.Decide

/** The Curry–Howard game, at a prompt.
  *
  * A console client over the same engine calls the Play screen will make, built
  * before the real interface so the *interaction* can be judged without any of
  * the design surface. Every panel here has a counterpart in the handoff's Play
  * screen: goal card, term card, resources, rules table.
  */
object Main:

  def main(args: Array[String]): Unit =
    println(banner)
    loop(State.start)

  // --- State ----------------------------------------------------------------

  final case class State(goal: Option[Goal], tree: Option[GameTree], view: View):
    def position: Option[Partial] = tree.map(_.current.position)

  object State:
    val start: State = State(None, None, View.Programmer)

  // --- The loop -------------------------------------------------------------

  private def loop(state: State): Unit =
    state.goal match
      case None       => loop(askForGoal(state))
      case Some(goal) => loop(play(state, goal))

  private def askForGoal(state: State): State =
    print("\nType a new signature > ")
    Option(StdIn.readLine()) match
      case None => quit()
      case Some(line) if isQuit(line) => quit()
      case Some(line) =>
        Parser.parseGoal(line) match
          case Left(error) =>
            println(s"  ${describe(error, line)}")
            state
          case Right(goal) =>
            // Say up front whether it can be finished at all. The oracle knows,
            // and letting a student grind at a non-theorem without saying so
            // would be a poor lesson — the negative ending is meant to be
            // reached on purpose (§4.7).
            if !Decide.provable(goal.formula) then
              println("  note: this is not an intuitionistic theorem — expect to get stuck.")
            state.copy(goal = Some(goal), tree = Some(GameTree.start(goal.formula)))

  private def play(state: State, goal: Goal): State =
    val tree = state.tree.get
    val position = tree.current.position

    position.status match
      case Status.Won => won(state, goal, position)
      case _ =>
        print("\n" + screen(state, goal, position))
        if position.status == Status.Dead then
          println("  ✗ dead end — some hole admits no move. `back` to a choice point.\n")

        print("> ")
        Option(StdIn.readLine()).map(_.trim) match
          case None                       => quit()
          case Some(line) if isQuit(line) => quit()
          case Some(line)                 => command(state, line)

  private def command(state: State, line: String): State =
    val tree = state.tree.get
    line.toLowerCase match
      case "" => state

      case "back" =>
        tree.current.parentId match
          case None =>
            println("  already at the opening position.\n")
            state
          case Some(parent) =>
            println(s"← back to move ${tree.current.depth - 1} of ${tree.size - 1}")
            println("  (the abandoned branch is still in `tree`)\n")
            state.copy(tree = Some(tree.goTo(parent)))

      case "view" =>
        val next = state.view.other
        println(s"  notation: ${next.toString.toLowerCase}  (the term stays Scala until Phase 7)\n")
        state.copy(view = next)

      case "tree" =>
        println(searchPath(tree))
        state

      case "goal" => State.start.copy(view = state.view)

      case "help" =>
        println(commands)
        state

      case other =>
        other.toIntOption.flatMap(n => Moves.offers(tree.current.position).find(_.number == n)) match
          case Some(offer) => state.copy(tree = Some(tree.play(offer.path, offer.moveIndex)))
          case None =>
            println(s"  not a move: $other   (try a number, or `help`)\n")
            state

  // --- Screens --------------------------------------------------------------

  private def screen(state: State, goal: Goal, position: Partial): String =
    List(
      View.goal(goal, state.view),
      View.program(position, state.view),
      View.resources(position, state.view),
      Moves.table(position, state.view),
      "  0) back      view  tree  goal  quit\n"
    ).mkString("\n")

  private def won(state: State, goal: Goal, position: Partial): State =
    val term = ToLambda.complete(position).get
    val moves = state.tree.get.current.depth
    val params = if goal.tyParams.isEmpty then "" else goal.tyParams.mkString("[", ", ", "]")

    println(s"\n✓ solved in $moves moves — the type is inhabited\n")
    println("  as built:")
    println(indent(ToScala(term), 4))
    println("\n  cleaned up:")
    println(indent(s"def solution$params = ${ToScala.bare(Cleanup.simplify(term))}", 4))
    // The check is independent of the rules that built the term, so it is worth
    // running even here.
    TypeCheck.check(term, goal.formula) match
      case Right(_)    => println("\n  type-checks against the goal.")
      case Left(error) => println(s"\n  ! the term does not check: $error")
    println()
    State.start.copy(view = state.view)

  private def searchPath(tree: GameTree): String =
    val rows = tree.nodes.values.toList.sortBy(_.id.value).map { node =>
      val here = if node.id == tree.currentId then "▸" else " "
      val move = node.via.fold("(start)")(key =>
        tree
          .node(node.parentId.get)
          .flatMap(p => p.position.movesAt(key.hole).lift(key.moveIndex))
          .fold("?")(NJ.label(_))
      )
      val holes = node.position.holes.length
      val status = node.position.status match
        case Status.Won  => "✓ solved"
        case Status.Dead => "✗ dead"
        case Status.Open => s"${holes}h"
      f"  $here ${node.id.value}%3d  ${"  " * node.depth}$move%-10s $status"
    }
    s"${View.panel("search path")}\n${rows.mkString("\n")}\n"

  // --- Odds and ends --------------------------------------------------------

  private def describe(error: ParseError, src: String): String =
    val where = if error.pos >= src.length then "at the end" else s"at position ${error.pos}"
    val what = error match
      case ParseError.Empty                  => "nothing to parse"
      case ParseError.UnknownChar(c, _)      => s"'$c' is not a symbol I know"
      case ParseError.ExpectedBracket(_)     => "Either needs a [ here"
      case ParseError.ExpectedComma(_)       => "expected a comma"
      case ParseError.ExpectedCloseBracket(_) => "expected a ]"
      case ParseError.ExpectedCloseParen(_)  => "expected a )"
      case ParseError.UnexpectedEnd(_)       => "the goal stops early"
      case ParseError.Unexpected(text, _)    => s"did not expect '$text'"
      case ParseError.TrailingInput(_)       => "there is more after the goal"
    s"$what, $where."

  private def indent(s: String, by: Int): String =
    s.linesIterator.map(" " * by + _).mkString("\n")

  private def isQuit(line: String): Boolean =
    Set("quit", "exit", ":q").contains(line.trim.toLowerCase)

  private def quit(): Nothing =
    println("\nbye.")
    sys.exit(0)

  private val banner: String =
    """
      |The Curry–Howard Game — console
      |Build a program and you have written a proof.
      |
      |A number plays a move.  Words do the rest: back  view  tree  goal  quit
      |Examples: (A, B) => (B, A)      a ∧ b → b ∧ a      Either[A, A => Nothing]
      |""".stripMargin

  private val commands: String =
    """
      |  <n>     play move n
      |  back    step to the previous position (nothing is lost; see `tree`)
      |  view    switch between the programmer's and the logician's notation
      |  tree    show the search path explored so far
      |  goal    start over with a new signature
      |  quit
      |""".stripMargin
