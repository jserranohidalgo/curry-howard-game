package curryhoward.engine
package form

import cats.Show
import Form.{Atom, And, Or, Implies}

/** The two readings of a formula.
  *
  * One structure, two interpretations — which is the whole Curry–Howard claim
  * rendered as code. Neither renderer can drift from the other, because there
  * is nothing for them to drift from: both fold the same formula.
  *
  * Precedence, after the prototype: atoms bind tightest, then `∧`, then `∨`,
  * then `→`, which is right-associative.
  */
object Notation:

  private val PrecAtom = 4
  private val PrecAnd = 3
  private val PrecOr = 2
  private val PrecImplies = 1

  /** `a ∧ (b ∨ c) → (a ∧ b) ∨ (a ∧ c)` — atoms lower-cased, `¬a` re-sugared. */
  def logician[F: Form]: Show[F] = Show.show(render(_, 0, logic = true))

  /** `((A, Either[B, C])) => Either[(A, B), (A, C)]` — atoms upper-cased. */
  def programmer[F: Form]: Show[F] = Show.show(render(_, 0, logic = false))

  private def render[F: Form](f: F, outer: Int, logic: Boolean): String =

    def wrap(s: String, prec: Int): String =
      if prec <= outer then s"($s)" else s

    f match
      case Atom(name)   => if logic then name.toLowerCase else name.capitalize
      case Form.True()  => if logic then "⊤" else "Unit"
      case Form.False() => if logic then "⊥" else "Nothing"

      case And(a, b) =>
        if logic then wrap(s"${render(a, PrecAnd, logic)} ∧ ${render(b, PrecAnd, logic)}", PrecAnd)
        else s"(${render(a, 0, logic)}, ${render(b, 0, logic)})"

      case Or(a, b) =>
        if logic then wrap(s"${render(a, PrecOr, logic)} ∨ ${render(b, PrecOr, logic)}", PrecOr)
        else s"Either[${render(a, 0, logic)}, ${render(b, 0, logic)}]"

      case Implies(a, b) =>
        b match
          // ¬a is a → ⊥. The parser accepts ¬ on input, so the printer gives it
          // back rather than exposing the desugaring.
          case Form.False() if logic =>
            wrap(s"¬${render(a, PrecAtom, logic)}", PrecAtom)
          case _ if logic =>
            wrap(
              s"${render(a, PrecImplies + 1, logic)} → ${render(b, PrecImplies - 1, logic)}",
              PrecImplies
            )
          case _ =>
            // Scala needs an extra bracket around a function domain, and around
            // a *product* domain too: `(P, Q) => R` is a two-argument function,
            // whereas the game means one argument that happens to be a pair.
            // Specification §4.9 writes `((P, Either[Q, R])) => …` for exactly
            // this reason.
            val dom = a match
              case Implies(_, _) | And(_, _) => s"(${render(a, 0, logic)})"
              case _                         => render(a, 0, logic)
            s"$dom => ${render(b, 0, logic)}"

      case _ => sys.error("unreachable: a formula has no other shape")
