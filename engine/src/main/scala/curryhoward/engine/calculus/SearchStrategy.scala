package curryhoward.engine
package calculus

import cats.*
import form.Form
import util.*
import NJ.given

/** How to pull complete derivations out of a search space.
  *
  * Not used to play the game — the player is the search strategy — but needed
  * for the things around it. Note the caveat on [[SearchSpace]]: this is fine
  * for shallow goals and exhausts the heap on anything deep, so questions of
  * the form "is this provable at all?" belong to the terminating LJT oracle
  * rather than here.
  */
trait SearchStrategy:
  def apply[F: Form](space: SearchSpace[F]): LazyList[Proof[F]]

object SearchStrategy:

  /** Depth-first. Complete only when the space is finite; on an infinite branch
    * it does not come back, which is the honest behaviour to expose rather than
    * to hide.
    */
  object depthFirst extends SearchStrategy:
    def apply[F: Form](space: SearchSpace[F]): LazyList[Proof[F]] =
      Mu.sequenceF[LazyList, [t] =>> NJ[F, t]](space)

  /** Iterative deepening up to `maxDepth`. Finds the shallowest derivations
    * first, and — because the bound is explicit — reports nothing rather than
    * diverging when the space is infinite.
    */
  case class iterativeDeepening(maxDepth: Int = 12) extends SearchStrategy:
    def apply[F: Form](space: SearchSpace[F]): LazyList[Proof[F]] =
      def go(depth: Int): LazyList[Proof[F]] =
        val found = Mu.sequenceF_Until[LazyList, [t] =>> NJ[F, t]](depth)(space)
        if found.nonEmpty then found
        else if depth >= maxDepth then LazyList.empty
        else go(depth + 1)
      go(1)

  val default: SearchStrategy = iterativeDeepening()
