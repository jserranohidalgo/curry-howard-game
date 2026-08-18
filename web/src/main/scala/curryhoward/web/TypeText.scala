package curryhoward.web

import com.raquo.laminar.api.L.{*, given}
import curryhoward.engine.ipl.*

/** A formula, printed with its atoms tinted.
  *
  * The design gives each atom a colour and keeps it — `P` is the same amber
  * wherever it appears, in the goal, in a hole, in a scope chip — so a player
  * can follow one proposition through a whole play without reading. The hues
  * are the system's own status colours, reused rather than invented (see the
  * `--atom-*` tokens).
  *
  * The tinting is done over the *printed* string rather than by a second
  * printer: `Notation` owns precedence and bracketing, and there is no reason
  * to have two things that can disagree about them. An identifier in the output
  * is an atom exactly when the formula has an atom of that name, which is what
  * `Goal.from` already collects.
  */
object TypeText:

  def apply(f: Formula, view: View): HtmlElement =
    span(cls := "ty", pieces(f, view))

  def logician(f: Formula): HtmlElement = apply(f, View.Logician)
  def programmer(f: Formula): HtmlElement = apply(f, View.Programmer)

  private def pieces(f: Formula, view: View): List[HtmlElement] =
    val text = view.show(f)
    val atoms = Goal.from(f).tyParams.toSet

    // Split into identifiers and everything else, then tint the identifiers
    // that name an atom of this formula — `Either`, `Unit` and `Nothing` are
    // not atoms, and neither is any operator.
    val tokens = "[A-Za-z][A-Za-z0-9_]*|[^A-Za-z]+".r.findAllIn(text).toList
    tokens.map { token =>
      val canonical = token.capitalize
      if atoms.contains(canonical) then
        span(cls := "atom", styleAttr := s"color: var(--atom-$canonical, inherit)", token)
      else span(token)
    }
