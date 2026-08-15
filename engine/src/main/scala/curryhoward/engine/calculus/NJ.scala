package curryhoward.engine
package calculus

import cats.{Applicative, Eval, Foldable, Traverse}
import cats.syntax.all.*
import form.Formula
import form.Formula.{And, Or, Implies}
import Sequent.Prem

/** Natural deduction, minimal — **what a proof is**.
  *
  * One case per rule of specification §3.1, in their general form: an
  * elimination takes a *derivation* of the thing it eliminates, not a variable.
  * There is no `let`, and no forward variant of anything. Both are derivable:
  * `let x: A = value in body` is the cut rule, and cut is
  * `(λx: A. body) value` — an implication introduced and immediately
  * eliminated.
  *
  * What a *move* is lives in [[Move]] and is a different question. A general
  * `→E` cannot be enumerated — given the goal `B`, the `A` in `A → B` is
  * unconstrained — so the moves the game offers are a curated set of skeletons
  * over these rules rather than the rules themselves. That separation is the
  * point rather than a compromise: §4.1's constitutive rules are precisely a
  * restriction of the permitted means.
  */
enum NJ[T]:
  case Ax(v: Prem)
  case TrueI()
  case FalseE(t: T, goal: Formula)
  case ImpliesI(param: Prem, body: T)
  case ImpliesE(fn: T, arg: T)
  case AndI(fst: T, snd: T)
  case AndE1(t: T)
  case AndE2(t: T)
  case OrI1(arg: T, rightType: Formula)
  case OrI2(arg: T, leftType: Formula)
  case OrE(scrutinee: T, left: Prem, onLeft: T, right: Prem, onRight: T)

object NJ:

  /** An interpretation of the rules: what a derivation *means*. */
  type Interp[T] = NJ[T] => T

  /** The name of the rule, as §3.1 writes it. */
  def label[T](rule: NJ[T]): String = rule match
    case Ax(_)                => "Ax"
    case TrueI()              => "⊤.I"
    case FalseE(_, _)         => "⊥.E"
    case ImpliesI(_, _)       => "⟶.I"
    case ImpliesE(_, _)       => "⟶.E"
    case AndI(_, _)           => "∧.I"
    case AndE1(_)             => "∧.E₁"
    case AndE2(_)             => "∧.E₂"
    case OrI1(_, _)           => "∨.I₁"
    case OrI2(_, _)           => "∨.I₂"
    case OrE(_, _, _, _, _)   => "∨.E"

  /** The sub-derivations, in the order they are filled. */
  def subgoals[T](rule: NJ[T]): List[T] = rule match
    case Ax(_)                       => Nil
    case TrueI()                     => Nil
    case FalseE(t, _)                => List(t)
    case ImpliesI(_, body)           => List(body)
    case ImpliesE(fn, arg)           => List(fn, arg)
    case AndI(fst, snd)              => List(fst, snd)
    case AndE1(t)                    => List(t)
    case AndE2(t)                    => List(t)
    case OrI1(arg, _)                => List(arg)
    case OrI2(arg, _)                => List(arg)
    case OrE(s, _, onLeft, _, onRight) => List(s, onLeft, onRight)

  given Traverse[NJ] with

    def traverse[G[_]: Applicative, A, B](fa: NJ[A])(f: A => G[B]): G[NJ[B]] =
      fa match
        case Ax(v)             => Applicative[G].pure(Ax(v))
        case TrueI()           => Applicative[G].pure(TrueI())
        case FalseE(t, g)      => f(t).map(FalseE(_, g))
        case ImpliesI(p, body) => f(body).map(ImpliesI(p, _))
        case ImpliesE(fn, arg) => (f(fn), f(arg)).mapN(ImpliesE(_, _))
        case AndI(fst, snd)    => (f(fst), f(snd)).mapN(AndI(_, _))
        case AndE1(t)          => f(t).map(AndE1(_))
        case AndE2(t)          => f(t).map(AndE2(_))
        case OrI1(arg, rt)     => f(arg).map(OrI1(_, rt))
        case OrI2(arg, lt)     => f(arg).map(OrI2(_, lt))
        case OrE(s, l, lb, r, rb) => (f(s), f(lb), f(rb)).mapN(OrE(_, l, _, r, _))

    def foldLeft[A, B](fa: NJ[A], b: B)(g: (B, A) => B): B =
      subgoals(fa).foldLeft(b)(g)

    def foldRight[A, B](fa: NJ[A], lb: Eval[B])(g: (A, Eval[B]) => Eval[B]): Eval[B] =
      Foldable[List].foldRight(subgoals(fa), lb)(g)
