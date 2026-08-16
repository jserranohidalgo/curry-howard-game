package curryhoward.engine
package ipl
package nj

import Partial.*
import NJ.given
import cats.*
import cats.syntax.all.*
import cats.free.Cofree
import util.*

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
type SearchSpace = Mu[[t] =>> LazyList[NJ[t]]]

/** A finished derivation: the same functor without the choice. */
type Proof = Mu[NJ]

object SearchSpace:

  type SearchF = [t] =>> LazyList[NJ[t]]

  given searchFunctor: Functor[SearchF] = Functor[LazyList].compose[NJ]

  /** The space rooted at a goal. */
  def apply(goal: Formula): SearchSpace = from(Sequent.initial(goal))

  /** The space rooted at an arbitrary hole — what the game needs when the
    * player is already mid-proof.
    */
  def from(hole: Sequent): SearchSpace =
    val unfolded: Sequent => SearchSpace = Mu.unfold[SearchF, Sequent](NJ.coalg)
    unfolded(hole)

  /** The space with every node still labelled by the hole it came from — the
    * search-path panel's data.
    */
  def trace(hole: Sequent): Cofree[SearchF, Sequent] =
    Cofree.unfold(hole)(NJ.coalg)

object Proof:

  extension (proof: Proof)
    /** Fold an interpretation over a finished derivation. */
    def interpret[T](interp: NJ.Interp[T]): T =
      val folded: Proof => T = Mu.fold[NJ, T](interp)
      folded(proof)
