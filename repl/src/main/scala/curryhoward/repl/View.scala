package curryhoward.repl

import curryhoward.engine.ipl.*
import curryhoward.engine.ipl.nj.*
import curryhoward.engine.ipl.nj.Partial.*

/** Which notation the panels are shown in.
  *
  * Only the *formulae* switch: goal, hole types, resource types. The term stays
  * Scala either way, because the logician's reading of a term is a
  * natural-deduction derivation and that renderer is Phase 7 — along with the
  * open question of how a `let` appears in it. Half a switch is still worth
  * having: it puts the correspondence between `p ∧ (q ∨ r)` and
  * `(P, Either[Q, R])` in front of the player every turn.
  */
enum View:
  case Programmer, Logician

  def show(f: Formula): String = this match
    case Programmer => Notation.programmer(f)
    case Logician   => Notation.logician(f)

  def other: View = if this == Programmer then Logician else Programmer

/** Drawing a position as four panels: goal, program, resources, moves.
  *
  * The same regions the Play screen has, flattened into one column — so what
  * is learned here about the interaction carries over to Phase 4 rather than
  * having to be discovered again.
  */
object View:

  private val Width = 66

  /** Holes are lettered and moves are numbered, so the two namespaces never
    * collide: `3) b Ax on q` says plainly which hole it acts on. With moves
    * offered across every hole at once there is no selection step, and every
    * move is a single keystroke.
    */
  def holeLabel(i: Int): String = ('a' + i).toChar.toString

  def panel(title: String): String =
    val dashes = "─" * math.max(0, Width - title.length - 5)
    s"─── $title $dashes"

  def goal(g: Goal, view: View): String =
    val params = if g.tyParams.isEmpty then "" else g.tyParams.mkString("[", ", ", "]")
    view match
      case Programmer =>
        s"${panel("goal")}\n  def solution$params:\n    ${view.show(g.formula)}\n"
      case Logician =>
        s"${panel("goal")}\n  ${view.show(g.formula)}\n"

  /** The program so far, with each hole marked by its letter. */
  def program(position: Partial, view: View): String =
    val holes = position.holes
    val rendered = ToScala(ToLambda.position(position))

    // The renderer writes each hole as `… : T`; label them left to right, which
    // is the order `holes` reports.
    val labelled = holes.zipWithIndex.foldLeft(rendered) { case (acc, ((_, hole), i)) =>
      acc.replaceFirst(
        java.util.regex.Pattern.quote(s"… : ${Notation.programmer(hole.con)}"),
        java.util.regex.Matcher.quoteReplacement(s"[${holeLabel(i)}] … : ${view.show(hole.con)}")
      )
    }
    s"${panel("program")}\n${layout(labelled).linesIterator.map("  " + _).mkString("\n")}\n"

  /** The renderer emits one line; a program is easier to read broken at its
    * bindings and its case arms, which is also how the design's term card
    * draws it.
    */
  private def layout(rendered: String): String =
    rendered
      .replace("; ", ";\n")
      .replace(" match { ", " match {\n  ")
      .replaceAll(" \\}$", "\n}")

  /** What is in scope, per hole. */
  def resources(position: Partial, view: View): String =
    val rows = position.holes.zipWithIndex.flatMap { case ((_, hole), i) =>
      if hole.ant.isEmpty then List(s"  [${holeLabel(i)}]  (nothing in scope)")
      else
        hole.ant.reverse.zipWithIndex.map { case ((_, ty), j) =>
          val marker = if j == 0 then s"  [${holeLabel(i)}] " else "      "
          f"$marker ${nameOf(position, i, j)}%-6s : ${view.show(ty)}%-34s ${kind(ty)}"
        }
    }
    s"${panel("resources")}\n${rows.mkString("\n")}\n"

  /** Resource names come from the renderer, so the panel and the program agree.
    * Falling back to the variable number is better than lying about a name.
    */
  private def nameOf(position: Partial, holeIndex: Int, resourceIndex: Int): String =
    val (_, hole) = position.holes(holeIndex)
    val scope = hole.ant.reverse
    ToScala.names(scope).getOrElse(scope(resourceIndex)._1, "?")

  private def kind(ty: Formula): String = ty match
    case Formula.And(_, _)     => "pair"
    case Formula.Or(_, _)      => "sum"
    case Formula.Implies(_, _) => "function"
    case Formula.Atom(_)       => "atom"
    case Formula.True          => "unit"
    case Formula.False         => "void"
