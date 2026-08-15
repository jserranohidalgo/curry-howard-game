package curryhoward.engine
package calculus

import cats.*
import form.Form
import util.*

/** How to pull complete derivations out of a search space.
  *
  * Not used to play the game — the player is the search strategy — but needed
  * for the things around it: deciding whether a goal is provable at all
  * (Phase 6's oracle), and later hints and auto-solve.
  */
trait SearchStrategy:
  def apply[F: Form, C[_, _]: Calculus](space: SearchSpace[F, C]): LazyList[Proof[F, C]]

object SearchStrategy:

  /** Depth-first. Complete only when the space is finite; on an infinite branch
    * it does not come back, which is the honest behaviour to expose rather than
    * to hide.
    */
  object depthFirst extends SearchStrategy:
    def apply[F: Form, C[_, _]: Calculus](space: SearchSpace[F, C]): LazyList[Proof[F, C]] =
      given Traverse[[t] =>> C[F, t]] = Calculus.traverse[F, C]
      Mu.sequenceF[LazyList, [t] =>> C[F, t]](space)

  /** Iterative deepening up to `maxDepth`. Finds the shallowest derivations
    * first, and — because the bound is explicit — reports nothing rather than
    * diverging when the space is infinite.
    */
  case class iterativeDeepening(maxDepth: Int = 12) extends SearchStrategy:
    def apply[F: Form, C[_, _]: Calculus](space: SearchSpace[F, C]): LazyList[Proof[F, C]] =
      given Traverse[[t] =>> C[F, t]] = Calculus.traverse[F, C]

      def go(depth: Int): LazyList[Proof[F, C]] =
        val found = Mu.sequenceF_Until[LazyList, [t] =>> C[F, t]](depth)(space)
        if found.nonEmpty then found
        else if depth >= maxDepth then LazyList.empty
        else go(depth + 1)

      go(1)

  val default: SearchStrategy = iterativeDeepening()
