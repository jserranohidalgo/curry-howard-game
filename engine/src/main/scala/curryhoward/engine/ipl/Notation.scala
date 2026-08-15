package curryhoward.engine
package ipl

import Formula.*

/** The two readings of a formula.
  *
  * Two functions over one type — which is the whole Curry–Howard claim rendered
  * as code, and needs no more machinery than this.
  *
  * Precedence, after the prototype: atoms bind tightest, then `∧`, then `∨`,
  * then `→`, which is right-associative.
  */
object Notation:

  private val PrecAtom = 4
  private val PrecAnd = 3
  private val PrecOr = 2
  private val PrecImplies = 1

  /** `p ∧ (q ∨ r) → p ∧ q ∨ p ∧ r` — atoms lower-cased, `¬a` re-sugared. */
  def logician(f: Formula): String = render(f, 0, logic = true)

  /** `((P, Either[Q, R])) => Either[(P, Q), (P, R)]` — atoms upper-cased. */
  def programmer(f: Formula): String = render(f, 0, logic = false)

  private def render(f: Formula, outer: Int, logic: Boolean): String =

    def wrap(s: String, prec: Int): String = if prec <= outer then s"($s)" else s

    f match
      case Atom(name) => if logic then name.toLowerCase else name.capitalize
      case True       => if logic then "⊤" else "Unit"
      case False      => if logic then "⊥" else "Nothing"

      case And(a, b) =>
        if logic then wrap(s"${render(a, PrecAnd, logic)} ∧ ${render(b, PrecAnd, logic)}", PrecAnd)
        else s"(${render(a, 0, logic)}, ${render(b, 0, logic)})"

      case Or(a, b) =>
        if logic then wrap(s"${render(a, PrecOr, logic)} ∨ ${render(b, PrecOr, logic)}", PrecOr)
        else s"Either[${render(a, 0, logic)}, ${render(b, 0, logic)}]"

      // ¬a is a → ⊥. The parser accepts ¬ on input, so the printer gives it
      // back rather than exposing the desugaring.
      case Implies(a, False) if logic =>
        wrap(s"¬${render(a, PrecAtom, logic)}", PrecAtom)

      case Implies(a, b) =>
        if logic then
          wrap(
            s"${render(a, PrecImplies + 1, logic)} → ${render(b, PrecImplies - 1, logic)}",
            PrecImplies
          )
        else
          // Scala needs an extra bracket around a function domain, and around a
          // *product* domain too: `(P, Q) => R` is a two-argument function,
          // whereas the game means one argument that happens to be a pair.
          // Specification §4.9 writes `((P, Either[Q, R])) => …` for this reason.
          val dom = a match
            case Implies(_, _) | And(_, _) => s"(${render(a, 0, logic)})"
            case _                         => render(a, 0, logic)
          s"$dom => ${render(b, 0, logic)}"
