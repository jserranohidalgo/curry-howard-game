package curryhoward.engine
package calculus

import cats.{Applicative, Eval, Foldable, Traverse}
import cats.syntax.all.*
import form.Formula
import form.Formula.{And, Or, Implies}
import Sequent.Prem

/** Natural deduction — **the rules of the game**.
  *
  * One case per cell of specification §3.2. Constructors act on the goal;
  * destructors act on a resource in scope, and come in two flavours, which is
  * where this parts company with any sequent calculus:
  *
  *   - **backward**, filling the hole outright when the resource's shape
  *     already matches it (`x._1` closing a `… : P`);
  *   - **forward**, binding the result as a new resource and carrying on
  *     (`val qr: Either[Q, R] = x._2`), which is how an intermediate value — in
  *     particular a scrutinee for `∨E` — is brought into scope.
  *
  * The type parameter is what sits at the sub-holes. During play it is
  * [[Partial]]; in a finished derivation it is another `NJ`; in an
  * interpretation it is whatever that interpretation produces. Everything the
  * engine does to a derivation is a fold with a different one.
  */
enum NJ[T]:

  // Constructors — build the goal's shape.
  case ImpliesI(param: Prem, body: T)
  case AndI(fst: T, snd: T)
  case OrI1(arg: T, rightType: Formula)
  case OrI2(arg: T, leftType: Formula)
  case TrueI()

  // Destructors — consume a resource in scope.
  case Ax(v: Prem)
  case FalseE(v: Prem, goal: Formula)
  case AndE1Back(v: Prem)
  case AndE2Back(v: Prem)
  case AndE1Fwd(v: Prem, bound: Prem, body: T)
  case AndE2Fwd(v: Prem, bound: Prem, body: T)
  case ImpliesEBack(v: Prem, arg: T)
  case ImpliesEFwd(v: Prem, arg: T, bound: Prem, body: T)
  case OrE(v: Prem, left: Prem, onLeft: T, right: Prem, onRight: T)

object NJ:

  /** An interpretation of the rules: what a derivation *means*. Rendering to
    * Scala, to a natural-deduction figure, or to a stored term are all this
    * with a different carrier.
    */
  type Interp[T] = NJ[T] => T

  private type Move = PartialFunction[Sequent, NJ[Sequent]]

  // --- The coalgebra: which moves are legal here -----------------------------

  /** Every move available at a hole, in the order the rules table lists them.
    *
    * Constructors see the goal once. Destructors see it once per resource in
    * scope — `Sequent.rotations` presents each resource at the head in turn,
    * so one rule yields one move per resource it applies to, which is what the
    * table's instance count shows.
    *
    * The projections are four separate rules rather than one parameterised by
    * a side. Partly because `∧.E₁` and `∧.E₂` are distinct cells and choosing
    * between them is the lesson; partly because a `PartialFunction` yields a
    * single move, so folding them together would let one silently shadow the
    * other whenever both components match the goal.
    *
    * A consumed resource stays in scope: `x` is still available after
    * `val qr = x._2`, and §4.9 relies on that when it closes a hole with
    * `x._1` several moves later.
    */
  def coalg(seq: Sequent): LazyList[NJ[Sequent]] =
    def fire(rules: List[Move], on: LazyList[Sequent]): LazyList[NJ[Sequent]] =
      LazyList.from(rules).flatMap(rule => on.flatMap(rule.lift(_)))

    fire(constructors, LazyList(seq)) ++
      fire(closers, seq.rotations) ++
      fire(openers, seq.rotations)

  /** Rules that build the goal's shape. */
  private val constructors: List[Move] = List(
    { case seq @ Sequent(ant, Implies(a, b)) =>
      val x = (seq.nextVar, a)
      ImpliesI(x, Sequent(x :: ant, b))
    },
    { case Sequent(ant, And(a, b)) => AndI(Sequent(ant, a), Sequent(ant, b)) },
    { case Sequent(ant, Or(a, b)) => OrI1(Sequent(ant, a), b) },
    { case Sequent(ant, Or(a, b)) => OrI2(Sequent(ant, b), a) },
    { case Sequent(_, Formula.True) => TrueI() }
  )

  /** Destructors that close the hole outright, opening nothing. */
  private val closers: List[Move] = List(
    { case Sequent((v, f) :: _, g) if f == g => Ax((v, f)) },
    { case Sequent((v, Formula.False) :: _, g) => FalseE((v, Formula.False), g) },
    { case Sequent((v, f @ And(a, _)) :: _, g) if a == g => AndE1Back((v, f)) },
    { case Sequent((v, f @ And(_, b)) :: _, g) if b == g => AndE2Back((v, f)) }
  )

  /** Destructors that open new holes, bind new resources, or both. */
  private val openers: List[Move] = List(
    { case Sequent((p @ (_, Implies(a, b))) :: gamma, g) if b == g =>
      ImpliesEBack(p, Sequent(p :: gamma, a))
    },
    { case seq @ Sequent((p @ (_, Or(a, b))) :: gamma, g) =>
      val l = (seq.nextVar, a)
      val r = (seq.nextVar + 1, b)
      OrE(p, l, Sequent(l :: p :: gamma, g), r, Sequent(r :: p :: gamma, g))
    },
    { case seq @ Sequent((p @ (_, And(a, _))) :: gamma, g) =>
      val bound = (seq.nextVar, a)
      AndE1Fwd(p, bound, Sequent(bound :: p :: gamma, g))
    },
    { case seq @ Sequent((p @ (_, And(_, b))) :: gamma, g) =>
      val bound = (seq.nextVar, b)
      AndE2Fwd(p, bound, Sequent(bound :: p :: gamma, g))
    },
    { case seq @ Sequent((p @ (_, Implies(a, b))) :: gamma, g) =>
      val bound = (seq.nextVar, b)
      ImpliesEFwd(p, Sequent(p :: gamma, a), bound, Sequent(bound :: p :: gamma, g))
    }
  )

  // --- Describing a move -----------------------------------------------------

  /** The name shown in the rules table and the search path. */
  def label[T](rule: NJ[T]): String = rule match
    case ImpliesI(_, _)          => "⟶.I"
    case AndI(_, _)              => "∧.I"
    case OrI1(_, _)              => "∨.I₁"
    case OrI2(_, _)              => "∨.I₂"
    case TrueI()                 => "⊤.I"
    case Ax(_)                   => "Ax"
    case FalseE(_, _)            => "⊥.E"
    case AndE1Back(_)            => "∧.E₁"
    case AndE2Back(_)            => "∧.E₂"
    case AndE1Fwd(_, _, _)       => "∧.E₁"
    case AndE2Fwd(_, _, _)       => "∧.E₂"
    case ImpliesEBack(_, _)      => "⟶.E"
    case ImpliesEFwd(_, _, _, _) => "⟶.E"
    case OrE(_, _, _, _, _)      => "∨.E"

  /** Whether the move binds its result as a new resource rather than filling
    * the hole with it.
    */
  def isForward[T](rule: NJ[T]): Boolean = rule match
    case AndE1Fwd(_, _, _) | AndE2Fwd(_, _, _) | ImpliesEFwd(_, _, _, _) => true
    case _                                                               => false

  /** The resource a destructor acts on, if it is one. */
  def actsOn[T](rule: NJ[T]): Option[Prem] = rule match
    case Ax(v)                   => Some(v)
    case FalseE(v, _)            => Some(v)
    case AndE1Back(v)            => Some(v)
    case AndE2Back(v)            => Some(v)
    case AndE1Fwd(v, _, _)       => Some(v)
    case AndE2Fwd(v, _, _)       => Some(v)
    case ImpliesEBack(v, _)      => Some(v)
    case ImpliesEFwd(v, _, _, _) => Some(v)
    case OrE(v, _, _, _, _)      => Some(v)
    case _                       => None

  /** The sub-holes a move opens, in the order they are filled. */
  def subgoals[T](rule: NJ[T]): List[T] = rule match
    case ImpliesI(_, body)             => List(body)
    case AndI(fst, snd)                => List(fst, snd)
    case OrI1(arg, _)                  => List(arg)
    case OrI2(arg, _)                  => List(arg)
    case TrueI()                       => Nil
    case Ax(_)                         => Nil
    case FalseE(_, _)                  => Nil
    case AndE1Back(_)                  => Nil
    case AndE2Back(_)                  => Nil
    case AndE1Fwd(_, _, body)          => List(body)
    case AndE2Fwd(_, _, body)          => List(body)
    case ImpliesEBack(_, arg)          => List(arg)
    case ImpliesEFwd(_, arg, _, body)  => List(arg, body)
    case OrE(_, _, onLeft, _, onRight) => List(onLeft, onRight)

  // --- Traversal -------------------------------------------------------------
  // Everything that folds or unfolds a derivation goes through this.

  given Traverse[NJ] with

    def traverse[G[_]: Applicative, A, B](fa: NJ[A])(f: A => G[B]): G[NJ[B]] =
      fa match
        case ImpliesI(p, body)    => f(body).map(ImpliesI(p, _))
        case AndI(fst, snd)       => (f(fst), f(snd)).mapN(AndI(_, _))
        case OrI1(arg, rt)        => f(arg).map(OrI1(_, rt))
        case OrI2(arg, lt)        => f(arg).map(OrI2(_, lt))
        case TrueI()              => Applicative[G].pure(TrueI())
        case Ax(v)                => Applicative[G].pure(Ax(v))
        case FalseE(v, g)         => Applicative[G].pure(FalseE(v, g))
        case AndE1Back(v)         => Applicative[G].pure(AndE1Back(v))
        case AndE2Back(v)         => Applicative[G].pure(AndE2Back(v))
        case AndE1Fwd(v, b, body) => f(body).map(AndE1Fwd(v, b, _))
        case AndE2Fwd(v, b, body) => f(body).map(AndE2Fwd(v, b, _))
        case ImpliesEBack(v, arg) => f(arg).map(ImpliesEBack(v, _))
        case ImpliesEFwd(v, arg, b, body) => (f(arg), f(body)).mapN(ImpliesEFwd(v, _, b, _))
        case OrE(v, l, lb, r, rb)         => (f(lb), f(rb)).mapN(OrE(v, l, _, r, _))

    def foldLeft[A, B](fa: NJ[A], b: B)(g: (B, A) => B): B =
      subgoals(fa).foldLeft(b)(g)

    def foldRight[A, B](fa: NJ[A], lb: Eval[B])(g: (A, Eval[B]) => Eval[B]): Eval[B] =
      Foldable[List].foldRight(subgoals(fa), lb)(g)
