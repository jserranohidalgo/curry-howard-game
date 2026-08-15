package curryhoward.engine
package util

import cats.*
import cats.syntax.all.*

/** Recursion-scheme plumbing, after `hablapps/tdd` (see /doc/tdd-lambdadays25.md).
  *
  * The engine leans on two of these. A **coalgebra** unfolds a game state into
  * the moves available at it — that is legal-move generation. An **algebra**
  * folds a finished derivation into a term — that is the program the player has
  * built. `Mu` ties the knot lazily, which is what lets an infinite search space
  * be represented without diverging.
  */
type Algebra[F[_], A] = F[A] => A

object Algebra:
  def apply[F[_], A](using A: Algebra[F, A]): Algebra[F, A] = A

type Coalgebra[A, F[_]] = A => F[A]

infix type Compose[F[_], G[_]] = [t] =>> F[G[t]]

/** The (lazy) fixpoint of a functor. */
case class Mu[F[_]](out: () => F[Mu[F]])

object Mu:

  def in[F[_]](f: => F[Mu[F]]): Mu[F] = Mu(() => f)

  def fold[F[_]: Functor, A](alg: Algebra[F, A]): Mu[F] => A =
    case Mu(f) => alg(f().map(fold(alg)))

  def unfold[F[_]: Functor, A](coalg: Coalgebra[A, F]): A => Mu[F] =
    a => Mu(() => coalg(a).map(unfold(coalg)))

  def hylo[F[_]: Functor, A, B](coalg: Coalgebra[A, F], alg: Algebra[F, B]): A => B =
    unfold(coalg) andThen fold(alg)

  def mapF[F[_]: Functor, G[_]](nat: F ~> G)(cf: Mu[F]): Mu[G] =
    Mu(() => nat(cf.out().map(mapF(nat))))

  /** Turn a fixpoint of `G ∘ H` into `G` of a fixpoint of `H` — the engine's
    * "collect the complete derivations out of the search space" step.
    */
  def sequenceF_Rec[G[_]: Monad, H[_]: Traverse](
      rec: Mu[G Compose H] => G[Mu[H]]
  )(ss: Mu[G Compose H]): G[Mu[H]] =
    Monad[G].flatMap(ss.out())(_.traverse(rec).map(Mu.in))

  def sequenceF[G[_]: Monad, H[_]: Traverse](ss: Mu[G Compose H]): G[Mu[H]] =
    sequenceF_Rec(sequenceF[G, H])(ss)

  /** Bounded version: gives up below `depth`. A search space that may be
    * infinite is only ever explored to a stated depth — the honest alternative
    * to pretending termination.
    */
  def sequenceF_Until[G[_]: Monad: Alternative, H[_]: Traverse](
      depth: Int
  )(ss: Mu[G Compose H]): G[Mu[H]] =
    if depth <= 0 then Alternative[G].empty
    else sequenceF_Rec(sequenceF_Until[G, H](depth - 1))(ss)
