package curryhoward.engine
package calculus

import cats.Traverse
import cats.syntax.all.*
import form.Form
import term.Term

/** A **game position**: a derivation under construction, with holes in it.
  *
  * Either an open hole waiting to be filled, or a rule application whose
  * sub-derivations are themselves partial. A finished game is one with no holes
  * left — at which point it folds to a term, and the term is the proof.
  */
enum Partial[F, C[_, _]]:
  case Open(hole: Sequent[F])
  case Node(rule: C[F, Partial[F, C]])

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

  def start[F: Form, C[_, _]](goal: F): Partial[F, C] =
    Open(Sequent.initial(goal))

  extension [F: Form, C[_, _]](p: Partial[F, C])(using C: Calculus[C])

    /** Every open hole, with its path, left to right — the order the player
      * meets them.
      */
    def holes: List[(Path, Sequent[F])] =
      given Traverse[[t] =>> C[F, t]] = C.traverse[F]
      p match
        case Open(hole) => List((Nil, hole))
        case Node(rule) =>
          rule.toList.zipWithIndex.flatMap: (child, i) =>
            child.holes.map((path, hole) => (i :: path, hole))

    def isComplete: Boolean = holes.isEmpty

    def holeAt(path: Path): Option[Sequent[F]] =
      holes.collectFirst { case (`path`, hole) => hole }

    /** Fill the hole at `path` with a move, opening its sub-holes.
      *
      * `mapWithIndex` rather than a hand-rolled counter: it is defined through
      * `traverse`, so its indices agree with `toList`'s order, which is what
      * `holes` reports paths against.
      */
    def fill(path: Path, move: C[F, Sequent[F]]): Partial[F, C] =
      given Traverse[[t] =>> C[F, t]] = C.traverse[F]
      path match
        case Nil =>
          p match
            case Open(_) => Node(move.map(Open(_)))
            case node    => node // already filled; nothing to do
        case i :: rest =>
          p match
            case Open(_) => p
            case Node(rule) =>
              Node(rule.mapWithIndex((child, j) => if j == i then child.fill(rest, move) else child))

    /** The term built so far, if there is nothing left to fill. */
    def term[T: Term.Aux[F]]: Option[T] =
      given Traverse[[t] =>> C[F, t]] = C.traverse[F]
      val algebra = Calculus.alg[F, T, C]
      def go(q: Partial[F, C]): Option[T] = q match
        case Open(_)    => None
        case Node(rule) => rule.traverse(go).map(algebra)
      go(p)

    /** The moves legal at a given hole. */
    def movesAt(path: Path): LazyList[C[F, Sequent[F]]] =
      holeAt(path).fold(LazyList.empty)(SearchSpace.moves[F, C])

    /** A hole with no legal move is a dead end — the branch cannot be
      * finished, though the game is not over (§4.7).
      */
    def deadHoles: List[Path] =
      holes.collect { case (path, hole) if SearchSpace.moves[F, C](hole).isEmpty => path }

    def status: Status =
      if isComplete then Status.Won
      else if deadHoles.nonEmpty then Status.Dead
      else Status.Open

/** How a position stands, evaluated after every move (§4.7, §6.2 of the
  * interaction spec).
  */
enum Status:
  /** No holes remain: the type is inhabited, the proposition proved. */
  case Won

  /** Some hole admits no move at all. Closes a branch, not the game. */
  case Dead

  /** Play continues. */
  case Open
