package curryhoward.web

import com.raquo.laminar.api.L.{*, given}
import curryhoward.engine.ipl.*
import curryhoward.engine.ipl.nj.*
import curryhoward.engine.ipl.nj.Figure.*

/** Drawing a natural-deduction figure (Phase 7, D25).
  *
  * The same structure the engine builds, with real rules instead of dashes:
  * premises in a row, a bar under them, the rule name to its right, the
  * conclusion beneath. A hole is drawn as `⋮` over the formula still to be
  * derived, and clicking it selects the same hole the programmer's view would —
  * which is what makes this a *view* of the game rather than a picture of it.
  */
object Derivation:

  /** @param onHole what to do when an open leaf is clicked, by hole index
    * @param selected which hole index is currently selected, if any
    */
  def apply(figure: Figure, onHole: Int => Unit, selected: Option[Int]): HtmlElement =
    figure match
      case Hyp(label, formula) =>
        div(
          cls := "fig-leaf mono",
          span(cls := "fig-hyp", s"[${Notation.logician(formula)}]"),
          sup(cls := "fig-label", label.toString)
        )

      case Todo(index, goal, assuming) =>
        div(
          cls := "fig-branch",
          // `[A]ⁿ` over the dots: §3.1 writes the premises of →I and ∨E that
          // way, and while the branch is empty it is the only place the
          // discharge label's meaning is visible.
          assuming.map { (label, formula) =>
            div(
              cls := "fig-leaf mono fig-assumed",
              span(cls := "fig-hyp", s"[${Notation.logician(formula)}]"),
              sup(cls := "fig-label", label.toString)
            )
          },
          // The dots are the *missing* derivation, so they sit outside the
          // frame: what is selected — and what a click picks up — is the
          // formula still to be proved, nothing else.
          div(cls := "fig-dots", "⋮"),
          div(
            cls := "fig-leaf fig-todo mono",
            cls("selected") := selected.contains(index),
            onClick --> { _ => onHole(index) },
            Notation.logician(goal)
          )
        )

      case Infer(rule, premises, conclusion, discharges) =>
        div(
          cls := "fig",
          div(cls := "fig-premises", premises.map(apply(_, onHole, selected))),
          div(
            cls := "fig-bar-row",
            div(cls := "fig-bar"),
            span(
              cls := "fig-rule mono",
              rule,
              if discharges.isEmpty then emptyNode
              else sup(cls := "fig-label", discharges.mkString(","))
            )
          ),
          div(cls := "fig-concl mono", Notation.logician(conclusion))
        )

  /** The facts standing beside the derivation — the logician's reading of the
    * resources panel (D25).
    *
    * Every entry is a small derivation whose conclusion is a formula now
    * available as a premise. An entry that has been grafted into the tree stays
    * here, marked: the shelf mirrors the resources panel entry for entry, which
    * is what keeps the two views in step.
    */
  def shelf(
      derived: List[Derived],
      names: Map[Int, String],
      onHole: Int => Unit,
      selected: Option[Int]
  ): HtmlElement =
    if derived.isEmpty then div()
    else
      div(
        cls := "derived",
        div(cls := "eyebrow", "Hechos derivados"),
        div(
          cls := "derived-list",
          derived.map { fact =>
            div(
              cls := "derived-fact",
              div(
                cls := "derived-head",
                span(cls := "mono strong", names.getOrElse(fact.binder, s"v${fact.binder}")),
                span(
                  cls := "kind",
                  fact.uses match
                    case 0 => "sin usar"
                    case 1 => "usado"
                    case n => s"usado ×$n"
                )
              ),
              apply(fact.figure, onHole, selected)
            )
          }
        )
      )
