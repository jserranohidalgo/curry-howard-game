package curryhoward.repl

import curryhoward.engine.ipl.*
import curryhoward.engine.ipl.nj.*
import curryhoward.engine.ipl.nj.Partial.*
import cats.syntax.all.*

/** One offered move: the number the player types, the hole it acts on, and how
  * to play it.
  */
final case class Offer(number: Int, holeIndex: Int, path: Partial.Path, moveIndex: Int, move: NJ[Sequent])

/** The rules table, as a menu.
  *
  * The design's claim is that the table *is* the syllabus, so it is drawn whole
  * every turn: rules that do not apply here are shown de-emphasised rather than
  * hidden, and rules that do not exist at all are shown as an absence. Those
  * are different things and the handoff is emphatic about it — `·` is "not
  * here", `—` is "there is no such rule".
  *
  * Moves span every open hole, so each entry carries its hole's letter.
  */
object Moves:

  /** Rows of §3.2, in the order the handoff draws them, plus `let` — which has
    * no cell in that table because it is neither an introduction nor an
    * elimination. Where it belongs in the real UI is an open question under
    * Phase 7; a row of its own is the honest answer for a console.
    */
  private val Rows: List[(String, List[String], List[String])] = List(
    // row label, construct labels, destruct labels
    ("=>", List("⟶.I"), List("⟶.E")),
    ("( , )", List("∧.I"), List("∧.E₁", "∧.E₂")),
    ("Either", List("∨.I₁", "∨.I₂"), List("∨.E")),
    ("Unit", List("⊤.I"), Nil),
    ("Nothing", Nil, List("⊥.E")),
    ("hyp", Nil, List("Ax")),
    ("let", List("let"), Nil)
  )

  /** Cells where no such rule exists: ⊥-introduction, ⊤-elimination,
    * hypothesis-introduction — and `let`, which is not a destructor at all.
    */
  private val Absent: Set[(String, Boolean)] =
    Set(
      ("Unit", false),    // ⊤ has no elimination
      ("Nothing", true),  // ⊥ has no introduction
      ("hyp", true),      // a hypothesis cannot be introduced
      ("let", false)      // a let is not a destructor
    )

  /** Number every legal move, across every open hole, left to right. */
  def offers(position: Partial): List[Offer] =
    position.holes.zipWithIndex
      .flatMap { case ((path, hole), holeIndex) =>
        NJ.coalg(hole).zipWithIndex.map((move, moveIndex) => (holeIndex, path, moveIndex, move))
      }
      .zipWithIndex
      .map { case ((holeIndex, path, moveIndex, move), n) =>
        Offer(n + 1, holeIndex, path, moveIndex, move)
      }

  def table(position: Partial, view: View): String =
    val all = offers(position)
    val holes = position.holes

    def entriesFor(labels: List[String]): List[String] =
      all
        .filter(o => labels.contains(NJ.label(o.move)))
        .map(o => render(o, holes(o.holeIndex)._2))

    val body = Rows.map { case (row, constructs, destructs) =>
      def column(labels: List[String], isConstruct: Boolean): List[String] =
        if Absent((row, isConstruct)) then List("—")
        else entriesFor(labels) match
          case Nil     => List("·")
          case entries => entries

      (row, column(constructs, true), column(destructs, false))
    }

    val header = f"${""}%-14s${"construct"}%-46s${"destruct"}"
    val lines = body.flatMap { case (row, left, right) =>
      val height = math.max(left.length, right.length)
      (0 until height).map { i =>
        val label = if i == 0 then row else ""
        val l = left.lift(i).getOrElse("")
        val r = right.lift(i).getOrElse("")
        f"  $label%-12s$l%-46s$r".stripTrailing()
      }
    }

    s"${View.panel("moves")}\n$header\n${lines.mkString("\n")}\n"

  /** `4) a val qr: Either[Q, R] = …` — the number, the hole, and what the move
    * does. The effect is the point: it is what the rules table's shape glyphs
    * convey visually, and it is the only thing that tells two `let`s apart,
    * since they differ solely in what they bind.
    */
  private def render(offer: Offer, hole: Sequent): String =
    val shape = effect(offer.move, hole)
    s"${offer.number}) ${View.holeLabel(offer.holeIndex)} $shape"

  private def effect(move: NJ[Sequent], hole: Sequent): String =
    // Rendered against the names already in force at the hole, so a move reads
    // `pqr._2` rather than `v0._2`.
    val known = ToScala.names(hole.ant.reverse)
    val asTerm = ToScala.fragment(ToLambda.apply(move.map(s => Lambda.Hole(s.con))), known)
    // A `let` renders as `val x: T = …; …`; the binding alone says what it does.
    // `val p: P = … : P` says P twice; the binding already gives the type.
    val shown =
      if NJ.binds(move).isDefined then asTerm.takeWhile(_ != ';').replaceAll("= … : .*$", "= …")
      else asTerm
    val trimmed = if shown.length > 42 then shown.take(41) + "…" else shown
    trimmed
