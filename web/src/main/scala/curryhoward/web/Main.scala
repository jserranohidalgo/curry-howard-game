package curryhoward.web

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import curryhoward.engine.ipl.*
import curryhoward.engine.ipl.Formula.Syntax.atom
import curryhoward.engine.ipl.ljt.Decide

/** Phase 1 shell. Renders a branded, empty page and nothing more — its job is
  * to prove that the build, the engine cross-build, Laminar and the static
  * assembly all work together. Phase 4 replaces it with Home → Setup → Play.
  */
object Main:

  def main(args: Array[String]): Unit =
    renderOnDomContentLoaded(dom.document.getElementById("app"), shell())

  private def shell(): HtmlElement =
    val detailsOpen = Var(false)

    div(
      cls := "sheet",
      img(cls := "hero-mark", src := "./assets/logo-urjc.svg", alt := "URJC"),
      h1(cls := "title", "The Curry–Howard Game"),
      p(
        cls := "tagline",
        "Un juego en el que construir un programa y demostrar un teorema son el mismo movimiento."
      ),
      p(
        cls := "lede",
        "Andamiaje de la fase 1: compilación, motor, Laminar y el empaquetado estático. ",
        "El juego llega en la fase 4."
      ),
      div(
        cls := "status",
        button(
          cls := "ghost",
          tpe := "button",
          onClick --> { _ => detailsOpen.update(!_) },
          child.text <-- detailsOpen.signal.map(if _ then "Ocultar detalles" else "Ver detalles")
        ),
        div(
          cls := "status-list",
          display <-- detailsOpen.signal.map(if _ then "block" else "none"),
          // The engine, running in the browser: LJT deciding provability.
          verdicts.map { goal =>
            statusRow(
              Notation.logician(goal),
              if Decide.provable(goal) then "demostrable" else "no demostrable"
            )
          }
        )
      )
    )

  /** A few goals for the engine to decide, live. Intuitionistic logic parts
    * company with classical logic on the last two.
    */
  private val verdicts: List[Formula] =
    val a = "A".atom
    val b = "B".atom
    List(
      a ==> a,
      (a /\ b) ==> (b /\ a),
      a \/ (a ==> Formula.False),          // excluded middle
      ((a ==> b) ==> a) ==> a              // Peirce's law
    )

  private def statusRow(label: String, value: String): HtmlElement =
    div(cls := "status-row", span(cls := "status-label", label), span(cls := "status-value", value))
