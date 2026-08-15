package curryhoward.engine
package ipl
package nj

import NJ.given
import cats.*
import util.*

/** How to pull complete derivations out of a search space.
  *
  * Not used to play the game — the player is the search strategy — but needed
  * for the things around it. Note the caveat on [[SearchSpace]]: this is fine
  * for shallow goals and exhausts the heap on anything deep, so questions of
  * the form "is this provable at all?" belong to the terminating LJT oracle
  * rather than here.
  */
trait SearchStrategy:
  def apply(space: SearchSpace): LazyList[Proof]

object SearchStrategy:

  /** Depth-first. Complete only when the space is finite; on an infinite branch
    * it does not come back, which is the honest behaviour to expose rather than
    * to hide.
    */
  object depthFirst extends SearchStrategy:
    def apply(space: SearchSpace): LazyList[Proof] =
      Mu.sequenceF[LazyList, NJ](space)

  /** Iterative deepening up to `maxDepth`. Finds the shallowest derivations
    * first, and — because the bound is explicit — reports nothing rather than
    * diverging when the space is infinite.
    */
  case class iterativeDeepening(maxDepth: Int = 12) extends SearchStrategy:
    def apply(space: SearchSpace): LazyList[Proof] =
      def go(depth: Int): LazyList[Proof] =
        val found = Mu.sequenceF_Until[LazyList, NJ](depth)(space)
        if found.nonEmpty then found
        else if depth >= maxDepth then LazyList.empty
        else go(depth + 1)
      go(1)

  val default: SearchStrategy = iterativeDeepening()
