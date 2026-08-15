package curryhoward.engine
package calculus

import form.Formula

/** Playing the game automatically.
  *
  * Not used to play it for real — the player is the search strategy — but
  * needed around the edges: proving that a goal *can* be finished, and building
  * test corpora.
  *
  * A completed position is a proof, so search is just "fill holes until there
  * are none", with a bound on how many moves that may take. The bound is not a
  * refinement: it is the only thing standing between this and non-termination,
  * because the move space is unbounded — every forward move adds a resource,
  * which multiplies the forward moves available next. Anything beyond shallow
  * goals exhausts the heap rather than the search space, which is why "is this
  * provable at all?" is a question for the terminating LJT oracle instead. See
  * Phase 6 in /Roadmap.md.
  */
object Search:

  /** Every way of finishing this position in at most `fuel` further moves,
    * lazily.
    */
  def completions(position: Partial, fuel: Int): LazyList[Partial] =
    position.holes.headOption match
      case None => LazyList(position)
      case Some((path, hole)) =>
        if fuel <= 0 then LazyList.empty
        else Move.at(hole).filter(worthTrying(hole)).flatMap(m => completions(position.fill(path, m), fuel - 1))

  /** Search-only pruning: skip a forward move that binds a type already in
    * scope.
    *
    * Holding two resources of the same type gains nothing, and offering the
    * move regardless is what makes the space unbounded — each forward move adds
    * a resource that licenses more forward moves. Without this, distributivity
    * takes half a minute to solve; with it, milliseconds.
    *
    * Applied **here and not in `Move.at`**, so gameplay is untouched: a player
    * may still take a redundant forward move and see where it goes. Whether the
    * restriction is *complete* — whether some goal needs a duplicate binding —
    * is exactly the question Phase 6 has to settle, and it is settled there
    * rather than assumed here.
    */
  private def worthTrying(hole: Sequent)(move: Move): Boolean =
    move.binds.forall(bound => !hole.ant.exists((_, ty) => ty == bound))

  /** Iterative deepening: the shallowest proof first, or nothing if none is
    * found within `maxMoves`. Nothing found is *not* a proof of unprovability.
    */
  def solve(goal: Formula, maxMoves: Int = 12): Option[Partial] =
    val start = Partial.start(goal)
    LazyList
      .range(1, maxMoves + 1)
      .flatMap(fuel => completions(start, fuel).headOption)
      .headOption

  /** As above, from a position already under way. */
  def solveFrom(position: Partial, maxMoves: Int = 12): Option[Partial] =
    LazyList
      .range(1, maxMoves + 1)
      .flatMap(fuel => completions(position, fuel).headOption)
      .headOption
