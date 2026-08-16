package curryhoward.engine
package ipl
package nj

import cats.Traverse
import cats.syntax.all.*
import util.*
import NJ.given

/** The functor a game position is the fixpoint of: a hole, or a layer of rules.
  *
  * `Either[Sequent, NJ[t]]` — so a position is `Mu[PartialF]`, which is the
  * free monad `Free[NJ, Sequent]`: holes are the variables, rule applications
  * are the layers. Writing it this way rather than as its own recursive `enum`
  * keeps the engine's rule intact — **recursion comes from the fixpoint and
  * nowhere else** — and it puts the three structures in one family, differing
  * only in what wraps `NJ`:
  *
  *   - `SearchSpace = Mu[[t] =>> LazyList[NJ[t]]]` — every play;
  *   - `Partial = Mu[[t] =>> Either[Sequent, NJ[t]]]` — a play in progress;
  *   - `Proof = Mu[NJ]` — a finished play.
  */
type PartialF = [t] =>> Either[Sequent, NJ[t]]

/** A **game position**: a derivation under construction, with holes in it.
  *
  * A finished game is one with no holes left — at which point it *is* a
  * [[Proof]], via [[Partial.toProof]], and folds to whatever an interpretation
  * makes of it.
  */
type Partial = Mu[PartialF]

object Partial:

  given traversePartialF: Traverse[PartialF] =
    Traverse[[t] =>> Either[Sequent, t]].compose[NJ]

  /** An unfilled hole. */
  object Open:
    def apply(hole: Sequent): Partial = Mu.in(Left(hole))
    def unapply(p: Partial): Option[Sequent] = p.out().left.toOption

  /** A rule applied, with its sub-derivations. */
  object Node:
    def apply(rule: NJ[Partial]): Partial = Mu.in(Right(rule))
    def unapply(p: Partial): Option[NJ[Partial]] = p.out().toOption

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

    /** Fold the position, saying what an open hole means.
      *
      * The catamorphism of `PartialF`, and the only recursion the other
      * readers need: `holes`, `toProof` and the renderings are all this with a
      * different carrier.
      */
    def fold[T](onHole: Sequent => T)(interp: NJ.Interp[T]): T =
      val cata: Partial => T = Mu.fold[PartialF, T] {
        case Left(hole) => onHole(hole)
        case Right(rule) => interp(rule)
      }
      cata(p)

    /** Every open hole, with its path, left to right — the order the player
      * meets them.
      *
      * A fold whose carrier is the holes found so far: each rule prefixes its
      * children's paths with the child's index, which is the same order
      * `NJ.subgoals` reports and so the same order `fill` walks.
      */
    def holes: List[(Path, Sequent)] =
      p.fold[List[(Path, Sequent)]](hole => List((Nil, hole))) { rule =>
        NJ.subgoals(rule).zipWithIndex.flatMap: (found, i) =>
          found.map((path, hole) => (i :: path, hole))
      }

    def isComplete: Boolean = holes.isEmpty

    def holeAt(path: Path): Option[Sequent] =
      holes.collectFirst { case (`path`, hole) => hole }

    /** The finished derivation, if there is nothing left to fill.
      *
      * `sequence` over the `Either` layer: a position with no holes is a
      * [[Proof]]. Interpretations then run on the proof, so there is one fold
      * for finished derivations rather than one here and another in `Proof`.
      */
    def toProof: Option[Proof] =
      p.fold[Option[Proof]](_ => None)(rule => rule.traverse(identity).map(Mu.in))

    /** The finished term, if there is nothing left to fill. */
    def term[T](interp: NJ.Interp[T]): Option[T] =
      toProof.map(Mu.fold[NJ, T](interp))

    /** Fill the hole at `path` with a move, opening its sub-holes.
      *
      * The one operation that is not a fold: it rewrites a *single* hole, and
      * holes are identified structurally by where they sit. (The free monad's
      * `flatMap` would rewrite them all at once, which is the right tool only
      * if holes carry identities — and paths were chosen over identities so
      * that a play serializes without a side table.)
      *
      * `mapWithIndex` rather than a hand-rolled counter: it is defined through
      * `traverse`, so its indices agree with the order `NJ.subgoals` reports.
      */
    def fill(path: Path, move: NJ[Sequent]): Partial = path match
      case Nil =>
        p match
          case Open(_) => Node(move.map(Open(_)))
          case node    => node // already filled; nothing to do
      case i :: rest =>
        p match
          case Open(_) => p
          case Node(rule) =>
            Node(rule.mapWithIndex((child, j) => if j == i then child.fill(rest, move) else child))
          case _ => p

    /** The moves legal at a given hole. */
    def movesAt(path: Path): LazyList[NJ[Sequent]] =
      holeAt(path).fold(LazyList.empty)(NJ.coalg)

    /** A hole with no legal move is a dead end — the branch cannot be
      * finished, though the game is not over (§4.7).
      */
    def deadHoles: List[Path] =
      holes.collect { case (path, hole) if NJ.coalg(hole).isEmpty => path }

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
