package curryhoward.engine
package calculus

import cats.syntax.all.*
import form.Formula
import NJ.given

/** A **game position**: a derivation under construction, with holes in it.
  *
  * Either an open hole waiting to be filled, or a rule application whose
  * sub-derivations are themselves partial. A finished game is one with no holes
  * left — at which point it folds to whatever an interpretation makes of it,
  * and what it makes of it is the program the player wrote.
  */
enum Partial:
  case Open(hole: Sequent)
  case Node(rule: NJ[Partial])

object Partial:

  /** Where a hole is: the path from the root, as the child index taken at each
    * step.
    *
    * Structural rather than a generated identifier, which means a hole's
    * identity survives being saved and reloaded, and a whole play can be
    * written down as a list of (path, move) pairs with no side table. D4/D5's
    * serialization and D6's shareable links both fall out of that.
    */
  type Path = List[Int]

  def start(goal: Formula): Partial = Open(Sequent.initial(goal))

  extension (p: Partial)

    /** Every open hole, with its path, left to right — the order the player
      * meets them.
      */
    def holes: List[(Path, Sequent)] = p match
      case Open(hole) => List((Nil, hole))
      case Node(rule) =>
        NJ.subgoals(rule).zipWithIndex.flatMap: (child, i) =>
          child.holes.map((path, hole) => (i :: path, hole))

    def isComplete: Boolean = holes.isEmpty

    def holeAt(path: Path): Option[Sequent] =
      holes.collectFirst { case (`path`, hole) => hole }

    /** Fill the hole at `path` with a move, opening its sub-holes.
      *
      * A move installs a whole skeleton, not a single node — a forward use is
      * three rules deep — so the holes it opens may sit several levels down.
      * Paths take care of themselves: they are read off the structure.
      *
      * `mapWithIndex` rather than a hand-rolled counter: it is defined through
      * `traverse`, so its indices agree with the order `NJ.subgoals` reports,
      * which is what `holes` builds paths against.
      */
    def fill(path: Path, move: Move): Partial = path match
      case Nil =>
        p match
          case Open(_) => move.skeleton
          case node    => node // already filled; nothing to do
      case i :: rest =>
        p match
          case Open(_) => p
          case Node(rule) =>
            Node(rule.mapWithIndex((child, j) => if j == i then child.fill(rest, move) else child))

    /** Fold the position with an interpretation, saying what an open hole
      * means. This is how a position is rendered *while* it still has holes —
      * the term card of the Play screen.
      */
    def fold[T](onHole: Sequent => T)(interp: NJ.Interp[T]): T =
      def go(q: Partial): T = q match
        case Open(hole) => onHole(hole)
        case Node(rule) => interp(rule.map(go))
      go(p)

    /** The finished term, if there is nothing left to fill. */
    def term[T](interp: NJ.Interp[T]): Option[T] =
      def go(q: Partial): Option[T] = q match
        case Open(_)    => None
        case Node(rule) => rule.traverse(go).map(interp)
      go(p)

    /** The moves legal at a given hole. */
    def movesAt(path: Path): LazyList[Move] =
      holeAt(path).fold(LazyList.empty)(Move.at)

    /** A hole with no legal move is a dead end — the branch cannot be
      * finished, though the game is not over (§4.7).
      */
    def deadHoles: List[Path] =
      holes.collect { case (path, hole) if Move.at(hole).isEmpty => path }

    def status: Status =
      if isComplete then Status.Won
      else if deadHoles.nonEmpty then Status.Dead
      else Status.Open

/** How a position stands, evaluated after every move (§4.7, and §6.2 of the
  * interaction spec).
  */
enum Status:
  /** No holes remain: the type is inhabited, the proposition proved. */
  case Won

  /** Some hole admits no move at all. Closes a branch, not the game. */
  case Dead

  /** Play continues. */
  case Open
