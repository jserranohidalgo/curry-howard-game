package curryhoward.engine
package interp

import form.Formula
import term.Lambda
import term.Lambda.*
import calculus.NJ
import calculus.NJ.*

/** The stored reading: a derivation as a plain term.
  *
  * This is the artifact the type checker audits. Checking a data type that
  * knows nothing about `NJ` is what makes the audit independent — an
  * interpretation that restated each rule's typing would be checking the
  * coalgebra against itself.
  *
  * Note how much of the rule set collapses here: a destructor is always a
  * variable with something done to it, so `Ax`, `∧.E₁` backward and the two
  * forward projections are all short compositions rather than new constructs.
  * That is why the game needs no term abstraction beyond this.
  */
object ToLambda:

  def apply: NJ.Interp[Lambda] =
    case ImpliesI(param, body) => Lam(param, body)
    case AndI(fst, snd)        => Pair(fst, snd)
    case OrI1(arg, rightType)  => InL(arg, rightType)
    case OrI2(arg, leftType)   => InR(arg, leftType)
    case TrueI()               => Unit

    case Ax((v, _))            => Var(v)
    case FalseE((v, _), goal)  => Absurd(Var(v), goal)
    case AndE1Back((v, _))     => Fst(Var(v))
    case AndE2Back((v, _))     => Snd(Var(v))

    case AndE1Fwd((v, _), bound, body) => Let(bound, Fst(Var(v)), body)
    case AndE2Fwd((v, _), bound, body) => Let(bound, Snd(Var(v)), body)

    case ImpliesEBack((v, _), arg)            => App(Var(v), arg)
    case ImpliesEFwd((v, _), arg, bound, body) => Let(bound, App(Var(v), arg), body)

    case OrE((v, _), left, onLeft, right, onRight) =>
      Match(Var(v), left, onLeft, right, onRight)
