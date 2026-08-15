package curryhoward.engine
package calculus

import form.Form
import term.Term

/** One inference rule, split the two ways the game needs it.
  *
  *   - `coalg` runs **bottom-up**: given a hole, does this rule apply, and what
  *     sub-holes does it open? That is legal-move generation, and partiality is
  *     the applicability test — a rule that does not match contributes nothing,
  *     which is exactly the greyed-out cell in the rules table.
  *   - `alg` runs **top-down**: given the sub-terms, how is this step of the
  *     program assembled?
  *
  * Because a rule is a value rather than a closure, a move can be stored,
  * replayed and serialized — which is what D4/D5 need and what the prototype's
  * `build()` function could never provide.
  */
trait Rule[R[_, _]]:

  def coalg[F: Form]: PartialFunction[Sequent[F], R[F, Sequent[F]]]

  def alg[F: Form, T: Term.Aux[F]]: R[F, T] => T

object Rule:

  def apply[R[_, _]](using R: Rule[R]): Rule[R] = R

  extension [A, B](f: PartialFunction[A, B])
    /** Apply a rule across candidate presentations of a hole, keeping the ones
      * where it fires.
      */
    def collectOn(candidates: LazyList[A]): LazyList[B] =
      candidates.flatMap(f.lift(_))
