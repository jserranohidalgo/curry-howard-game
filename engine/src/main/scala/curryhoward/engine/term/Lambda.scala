package curryhoward.engine
package term

import form.Formula

/** A term, as data.
  *
  * One node per shape a derivation can produce — see `interp.ToLambda`, which
  * is the interpretation that builds these. It exists to be *checked*: the type
  * checker works on this and knows nothing about the rules, so the two agree
  * only if both are right.
  *
  * There is no `let`. A forward move builds `(λx: A. body) value`, which is
  * what a `let` is; `interp.ToScala` recognises that shape and prints it back
  * as `val x: A = value; body`, which is what Scala's `val` means. Binders stay
  * explicit on `Lam` and `Match` because the game shows them — `case Left(q) =>`
  * is on screen, and the name is part of the lesson.
  */
enum Lambda:
  case Var(v: Int)
  case Unit
  case Lam(param: (Int, Formula), body: Lambda)
  case App(f: Lambda, arg: Lambda)
  case Pair(fst: Lambda, snd: Lambda)
  case Fst(t: Lambda)
  case Snd(t: Lambda)
  case InL(t: Lambda, rightType: Formula)
  case InR(t: Lambda, leftType: Formula)
  case Match(
      scrutinee: Lambda,
      left: (Int, Formula),
      onLeft: Lambda,
      right: (Int, Formula),
      onRight: Lambda
  )
  case Absurd(t: Lambda, goal: Formula)

  /** An unfilled hole, with the type it must eventually have. A position
    * mid-game is a term with these in it.
    */
  case Hole(goal: Formula)

object Lambda:
  given cats.Eq[Lambda] = cats.Eq.fromUniversalEquals
