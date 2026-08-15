package curryhoward.engine
package calculus

import cats.*
import form.Form
import term.Term
import util.*

/** A whole rule set, as one value.
  *
  * This is what "the engine is parameterised by the calculus" means in
  * practice: the game plays `Calculus[NJ]`, the oracle decides with
  * `Calculus[LJT]`, and a future logic is another instance rather than a fork.
  */
trait Calculus[C[_, _]]:

  /** Needed to fold and unfold the rule functor. */
  given traverse[F: Form]: Traverse[[t] =>> C[F, t]]

  /** All moves available at a hole, each carrying the holes it opens. */
  def coalg[F: Form]: Coalgebra[Sequent[F], SearchSpace.SearchF[F, C]]

  /** How a step of the program is built once its sub-terms are known. */
  def alg[F: Form, T: Term.Aux[F]]: Algebra[[t] =>> C[F, t], T]

  /** A label for the rule, for the rules table and the search path. */
  def label[F: Form, T](c: C[F, T]): String

object Calculus:

  def apply[C[_, _]](using C: Calculus[C]): Calculus[C] = C

  given traverse[F: Form, C[_, _]](using C: Calculus[C]): Traverse[[t] =>> C[F, t]] =
    C.traverse[F]

  def coalg[F: Form, C[_, _]](using C: Calculus[C]): Coalgebra[Sequent[F], SearchSpace.SearchF[F, C]] =
    C.coalg[F]

  def alg[F: Form, T: Term.Aux[F], C[_, _]](using C: Calculus[C]): Algebra[[t] =>> C[F, t], T] =
    C.alg[F, T]
