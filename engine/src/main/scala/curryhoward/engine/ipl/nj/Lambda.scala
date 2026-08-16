package curryhoward.engine
package ipl
package nj


/** A term, as data.
  *
  * One node per shape a derivation can produce — see `interp.ToLambda`, which
  * is the interpretation that builds these. It exists to be *checked*: the type
  * checker works on this and knows nothing about the rules, so the two agree
  * only if both are right.
  *
  * Binders are explicit on `Match` and `Let` because the game shows them:
  * `case Left(q) => …` and `val qr: Either[Q, R] = pqr._2` are on screen, and
  * those names are part of the lesson. A calculus that only needed to *prove*
  * things could substitute them away.
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
  case Let(binder: (Int, Formula), value: Lambda, body: Lambda)
  case Absurd(t: Lambda, goal: Formula)

  /** An unfilled hole, with the type it must eventually have. A position
    * mid-game is a term with these in it, which is what the Play screen shows.
    */
  case Hole(goal: Formula)

object Lambda:
  given cats.Eq[Lambda] = cats.Eq.fromUniversalEquals
