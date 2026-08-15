package curryhoward.engine
package interp

import form.Formula
import term.Lambda
import term.Lambda.*
import calculus.{NJ, Partial}
import calculus.NJ.*

/** The stored reading: a derivation as a plain term.
  *
  * This is the artifact the type checker audits. Checking a data type that
  * knows nothing about `NJ` is what makes the audit independent — an
  * interpretation that restated each rule's typing would be checking the
  * coalgebra against itself.
  *
  * With the calculus minimal, this is very nearly the identity: one term node
  * per rule. The interesting work has moved to where it belongs — `Move`
  * decides which skeletons are offered, and `ToScala` decides how a `let`
  * skeleton is written back out.
  */
object ToLambda:

  def apply: NJ.Interp[Lambda] =
    case Ax((v, _))            => Var(v)
    case TrueI()               => Unit
    case FalseE(t, goal)       => Absurd(t, goal)
    case ImpliesI(param, body) => Lam(param, body)
    case ImpliesE(fn, arg)     => App(fn, arg)
    case AndI(fst, snd)        => Pair(fst, snd)
    case AndE1(t)              => Fst(t)
    case AndE2(t)              => Snd(t)
    case OrI1(arg, rightType)  => InL(arg, rightType)
    case OrI2(arg, leftType)   => InR(arg, leftType)

    case OrE(scrutinee, left, onLeft, right, onRight) =>
      Match(scrutinee, left, onLeft, right, onRight)

  /** A position, holes and all. */
  def position(p: Partial): Lambda = p.fold(h => Hole(h.con))(apply)

  /** The finished term, if the position has no holes left. */
  def complete(p: Partial): Option[Lambda] = p.term(apply)
