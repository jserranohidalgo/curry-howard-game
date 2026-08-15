package curryhoward.engine
package calculus

import cats.*
import cats.syntax.all.*
import cats.free.Cofree
import form.Form
import term.Term
import util.*

/** The space of all plays, unfolded lazily.
  *
  * Every node is a hole; its children are the moves legal at it, each with the
  * holes that move opens. Nothing is computed until it is looked at, which is
  * what lets a space that may be infinite — the double-negation-elimination
  * case of specification §4.10 — be represented at all.
  *
  * The *game tree* the player builds is a finite path-set through this space.
  */
type SearchSpace[F, C[_, _]] = Mu[[t] =>> LazyList[C[F, t]]]

/** A finished derivation: the same functor without the choice. */
type Proof[F, C[_, _]] = Mu[[t] =>> C[F, t]]

object SearchSpace:

  type SearchF[F, C[_, _]] = [t] =>> LazyList[C[F, t]]

  /** Derived from the calculus rather than from a bare `Functor`: with only a
    * `Functor[[t] =>> C[F, t]]` asked for, inference wanders off into cats'
    * `Arrow`-based instances and fails.
    */
  given searchFunctor[F: Form, C[_, _]](using C: Calculus[C]): Functor[SearchF[F, C]] =
    Functor[LazyList].compose[[t] =>> C[F, t]](using C.traverse[F])

  // Note on style: each of these binds the coalgebra to a local first. Applied
  // inline, `Calculus.coalg[F, C](hole)` reads as passing `hole` *as* the
  // `Form[F]` evidence — Scala 3 allows using-clauses to be supplied
  // positionally, and picks that reading over the intended one.

  /** The space rooted at a goal. */
  def apply[F: Form, C[_, _]: Calculus](goal: F): SearchSpace[F, C] =
    from(Sequent.initial(goal))

  /** The space rooted at an arbitrary hole — what the game needs when the
    * player is already mid-proof.
    */
  def from[F: Form, C[_, _]: Calculus](hole: Sequent[F]): SearchSpace[F, C] =
    val co: Coalgebra[Sequent[F], SearchF[F, C]] = Calculus.coalg[F, C]
    val unfolded: Sequent[F] => SearchSpace[F, C] = Mu.unfold(co)
    unfolded(hole)

  /** The space with every node still labelled by the hole it came from — the
    * search-path panel's data.
    */
  def trace[F: Form, C[_, _]: Calculus](hole: Sequent[F]): Cofree[SearchF[F, C], Sequent[F]] =
    val co: Coalgebra[Sequent[F], SearchF[F, C]] = Calculus.coalg[F, C]
    Cofree.unfold(hole)(co)

  /** The moves legal at a hole, in one step. This is `legalMoves` from the
    * prototype, and it is the only entry point the Play screen needs.
    */
  def moves[F: Form, C[_, _]: Calculus](hole: Sequent[F]): LazyList[C[F, Sequent[F]]] =
    val co: Coalgebra[Sequent[F], SearchF[F, C]] = Calculus.coalg[F, C]
    co(hole)

object Proof:

  extension [F: Form, C[_, _]: Calculus](proof: Proof[F, C])
    /** Fold the calculus algebra over a finished derivation to get its term —
      * the program the player wrote, which is the proof they built.
      */
    def program[T: Term.Aux[F]]: T =
      given Functor[[t] =>> C[F, t]] = Calculus.traverse[F, C]
      val fold: Proof[F, C] => T = Mu.fold(Calculus.alg[F, T, C])
      fold(proof)
