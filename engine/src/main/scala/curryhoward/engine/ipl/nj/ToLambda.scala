package curryhoward.engine
package ipl
package nj

import Partial.*
import NJ.*
import Lambda.*

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

    case ImpliesEBack((v, _), arg) => App(Var(v), arg)

    // Forward reasoning: bind a value, then carry on with it in scope.
    case NJ.Let(bound, value, body) => Lambda.Let(bound, value, body)

    case OrE((v, _), left, onLeft, right, onRight) =>
      Match(Var(v), left, onLeft, right, onRight)

  /** A position, holes and all — what the term panel shows mid-game. */
  def position(p: Partial): Lambda = p.fold(h => Lambda.Hole(h.con))(apply)

  /** The finished term, if the position has no holes left. */
  def complete(p: Partial): Option[Lambda] = p.term(apply)
