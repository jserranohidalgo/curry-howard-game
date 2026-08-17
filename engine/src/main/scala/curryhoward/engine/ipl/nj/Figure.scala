package curryhoward.engine
package ipl
package nj

/** A natural-deduction figure: the logician's reading of a position (D25).
  *
  * The shape is §3.1's and nothing else — premises over a bar, the rule name to
  * its right, hypotheses bracketed and labelled, and the labels a rule
  * discharges written on it. There is deliberately no node for a cut: `let` is
  * not a rule of natural deduction, and the figure never contains one (see
  * [[ToFigure]] for where a `let` goes instead).
  */
enum Figure:

  /** A bar. Premises are in the order the rule presents them — for `∨E` the
    * major premise first, as §3.1 draws it — which is *not* always the order
    * the game fills the holes in.
    */
  case Infer(rule: String, premises: List[Figure], derives: Formula, discharges: List[Int])

  /** An assumption in force, bracketed and labelled: `[A]¹`. The label is
    * discharged by the rule that carries the same number.
    */
  case Hyp(label: Int, formula: Formula)

  /** A leaf that is not derived yet — the logician's hole. `index` is its
    * position in the position's hole list, so a click on it selects the same
    * hole the programmer's view would.
    *
    * `assuming` carries the hypotheses a discharging rule has just put in force
    * over this branch, drawn above it as `[A]ⁿ ⋮ C` — which is how §3.1 writes
    * the premises of `→I` and `∨E`, and the only place a label's meaning is
    * visible while the branch is still empty.
    *
    * A hole can appear **twice** in one figure: it belongs to a derived fact
    * that has been grafted at two use sites, and both drawings are the same
    * hole.
    */
  case Todo(index: Int, goal: Formula, assuming: List[(Int, Formula)] = Nil)

  def conclusion: Formula = this match
    case Infer(_, _, c, _) => c
    case Hyp(_, f)         => f
    case Todo(_, goal, _)  => goal

  /** Every hole drawn in this figure, in the order it is drawn. */
  def holes: List[Int] = this match
    case Infer(_, premises, _, _) => premises.flatMap(_.holes)
    case Hyp(_, _)                => Nil
    case Todo(index, _, _)        => List(index)

object Figure:

  /** A fact derived by a forward move, waiting on the shelf beside the main
    * derivation (D25).
    *
    * `binder` is the variable the programmer's view names, so the two panels
    * can be lined up entry for entry. `uses` counts how often the fragment has
    * been grafted into the tree: zero means it is only on the shelf, and more
    * than one means it is drawn more than once — a derivation is a tree, and
    * sharing is a property of the program, not of the proof.
    */
  final case class Derived(binder: Int, formula: Formula, figure: Figure, uses: Int)

  /** What the logician view shows: one derivation, and the facts standing
    * beside it.
    */
  final case class Forest(main: Figure, derived: List[Derived]):
    def isBare: Boolean = derived.isEmpty

  // --- Drawing it ------------------------------------------------------------

  /** The figure as text, in the layout of the specification's §4.9: premises
    * side by side, a bar under them, the rule name to the right, the conclusion
    * centred underneath.
    *
    * Written for the console client and for tests to read — the web view draws
    * the same structure with real rules and boxes — and it is the cheapest way
    * to see that a figure is right.
    */
  def ascii(figure: Figure, notation: Formula => String = Notation.logician): Vector[String] =
    block(figure, notation).lines

  /** A rendered block: lines of equal width, with the conclusion on the last
    * one. Premises are joined bottom-up, so every conclusion sits on the bar
    * that consumes it.
    */
  private final case class Block(lines: Vector[String]):
    def width: Int = lines.map(_.length).maxOption.getOrElse(0)
    def height: Int = lines.length
    def padTo(w: Int): Block = Block(lines.map(l => l + " " * (w - l.length)))
    def raiseTo(h: Int): Block = Block(Vector.fill(h - height)(" " * width) ++ lines)

  private def centre(text: String, width: Int): String =
    val left = (width - text.length) / 2
    " " * left.max(0) + text

  private def beside(blocks: List[Block]): Block =
    if blocks.isEmpty then Block(Vector.empty)
    else
      val h = blocks.map(_.height).max
      val raised = blocks.map(b => b.padTo(b.width).raiseTo(h))
      Block(
        (0 until h).toVector.map(i => raised.map(_.lines(i)).mkString("   "))
      )

  private def block(figure: Figure, show: Formula => String): Block = figure match
    case Hyp(label, formula)  => Block(Vector(s"[${show(formula)}]${superscript(label)}"))
    case Todo(_, goal, assuming) =>
      val text = show(goal)
      val hyps = assuming.map((n, f) => s"[${show(f)}]${superscript(n)}").mkString(" ")
      val width = text.length.max(hyps.length)
      Block(
        (if hyps.isEmpty then Vector.empty else Vector(centre(hyps, width)))
          ++ Vector(centre("⋮", width), centre(text, width))
      )
    case Infer(rule, premises, conclusion, discharges) =>
      val above = beside(premises.map(block(_, show)))
      val name = rule + discharges.map(superscript).mkString
      val text = show(conclusion)
      val barWidth = above.width.max(text.length)
      val padded = above.padTo(barWidth)
      Block(
        padded.lines
          :+ ("─" * barWidth + " " + name)
          :+ centre(text, barWidth)
      )

  private def superscript(n: Int): String =
    n.toString.map(d => "⁰¹²³⁴⁵⁶⁷⁸⁹".charAt(d - '0'))
