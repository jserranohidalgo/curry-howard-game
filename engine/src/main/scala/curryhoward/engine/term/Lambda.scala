package curryhoward.engine
package term

import cats.Show
import form.Formula

/** The initial interpretation of [[Term]]: the term the game stores.
  *
  * Every node corresponds to one move from specification §3.2, which is what
  * makes a finished game and a finished proof the same object.
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

  def subst(v: Int, by: Lambda): Lambda = this match
    case Var(`v`)         => by
    case _: Var           => this
    case Unit             => this
    case Lam((`v`, _), _) => this // shadowed
    case Lam(p, b)        => Lam(p, b.subst(v, by))
    case App(f, a)        => App(f.subst(v, by), a.subst(v, by))
    case Pair(a, b)       => Pair(a.subst(v, by), b.subst(v, by))
    case Fst(t)           => Fst(t.subst(v, by))
    case Snd(t)           => Snd(t.subst(v, by))
    case InL(t, r)        => InL(t.subst(v, by), r)
    case InR(t, l)        => InR(t.subst(v, by), l)
    case Match(s, l, lb, r, rb) =>
      Match(
        s.subst(v, by),
        l,
        if l._1 == v then lb else lb.subst(v, by),
        r,
        if r._1 == v then rb else rb.subst(v, by)
      )
    case Let(b, value, body) =>
      Let(b, value.subst(v, by), if b._1 == v then body else body.subst(v, by))
    case Absurd(t, g) => Absurd(t.subst(v, by), g)

object Lambda:

  given Term[Formula, Lambda] with

    extension (v: Int) def `var`: Lambda = Var(v)

    val unit: Lambda = Unit

    extension (p: (Int, Formula)) def lam(body: Lambda): Lambda = Lam(p, body)

    def matchOn(
        scrutinee: Lambda,
        left: (Int, Formula),
        onLeft: Lambda,
        right: (Int, Formula),
        onRight: Lambda
    ): Lambda = Match(scrutinee, left, onLeft, right, onRight)

    def let(binder: (Int, Formula), value: Lambda, body: Lambda): Lambda =
      Let(binder, value, body)

    extension (t: Lambda)
      infix def and(t2: Lambda): Lambda = Pair(t, t2)
      def _1: Lambda = Fst(t)
      def _2: Lambda = Snd(t)
      def inl(rightType: Formula): Lambda = InL(t, rightType)
      def inr(leftType: Formula): Lambda = InR(t, leftType)
      def apply(arg: Lambda): Lambda = App(t, arg)
      def absurd(goal: Formula): Lambda = Absurd(t, goal)
      def subst(v: Int, by: Lambda): Lambda = t.subst(v, by)

  given cats.Eq[Lambda] = cats.Eq.fromUniversalEquals

  /** Default rendering is the programmer's — see [[Rendering]]. */
  given Show[Lambda] = Rendering.scala
