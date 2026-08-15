package curryhoward.engine
package calculus

import cats.*
import cats.syntax.all.*
import cats.free.Cofree
import form.Form
import util.*
import NJ.given

/** The space of all plays, unfolded lazily.
  *
  * Every node is a hole; its children are the moves legal at it, each with the
  * holes that move opens. Nothing is computed until it is looked at, which is
  * what lets a space that may be infinite — the double-negation-elimination
  * case of specification §4.10 — be represented at all.
  *
  * The *game tree* the player builds is a finite path-set through this space.
  * Searching it exhaustively is a different matter: every forward move adds a
  * resource, which multiplies the forward moves available next, so the
  * branching factor grows without bound. Search is usable for shallow goals and
  * not much else — see Phase 6 in /Roadmap.md.
  */
type SearchSpace[F] = Mu[[t] =>> LazyList[NJ[F, t]]]

/** A finished derivation: the same functor without the choice. */
type Proof[F] = Mu[[t] =>> NJ[F, t]]

object SearchSpace:

  type SearchF[F] = [t] =>> LazyList[NJ[F, t]]

  given searchFunctor[F]: Functor[SearchF[F]] =
    Functor[LazyList].compose[[t] =>> NJ[F, t]]

  /** The space rooted at a goal. */
  def apply[F: Form](goal: F): SearchSpace[F] = from(Sequent.initial(goal))

  /** The space rooted at an arbitrary hole — what the game needs when the
    * player is already mid-proof.
    */
  def from[F: Form](hole: Sequent[F]): SearchSpace[F] =
    val unfolded: Sequent[F] => SearchSpace[F] = Mu.unfold[SearchF[F], Sequent[F]](NJ.coalg[F])
    unfolded(hole)

  /** The space with every node still labelled by the hole it came from — the
    * search-path panel's data.
    */
  def trace[F: Form](hole: Sequent[F]): Cofree[SearchF[F], Sequent[F]] =
    Cofree.unfold(hole)(NJ.coalg[F])

object Proof:

  extension [F: Form](proof: Proof[F])
    /** Fold an interpretation over a finished derivation. */
    def interpret[T](interp: NJ.Interp[F, T]): T =
      val folded: Proof[F] => T = Mu.fold[[t] =>> NJ[F, t], T](interp)
      folded(proof)
