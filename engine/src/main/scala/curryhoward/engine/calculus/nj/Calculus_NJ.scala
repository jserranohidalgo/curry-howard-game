package curryhoward.engine
package calculus
package nj

import cats.*
import cats.syntax.all.*
import form.Form
import term.Term
import util.*
import NJ.*
import Rule.collectOn

/** The game's calculus: every rule of §3.2, assembled into one value.
  *
  * The order rules are listed in is the order moves appear in, so it follows
  * the rules table top to bottom — constructors first, then the destructors
  * that close a hole outright, then the ones that open new work.
  */
object Calculus_NJ extends Calculus[NJ]:

  def coalg[F: Form]: Coalgebra[Sequent[F], SearchSpace.SearchF[F, NJ]] = seq =>
    val here = LazyList(seq)
    val perResource = seq.rotations

    // Constructors: one presentation of the goal, so `here`.
    Rule[ImpliesI].coalg[F].collectOn(here) ++
      Rule[AndI].coalg[F].collectOn(here) ++
      Rule[OrI1].coalg[F].collectOn(here) ++
      Rule[OrI2].coalg[F].collectOn(here) ++
      Rule[TrueI].coalg[F].collectOn(here) ++
      // Destructors: once per resource in scope.
      Rule[Ax].coalg[F].collectOn(perResource) ++
      Rule[FalseE].coalg[F].collectOn(perResource) ++
      Rule[AndE1Back].coalg[F].collectOn(perResource) ++
      Rule[AndE2Back].coalg[F].collectOn(perResource) ++
      Rule[ImpliesEBack].coalg[F].collectOn(perResource) ++
      Rule[OrE].coalg[F].collectOn(perResource) ++
      Rule[AndE1Fwd].coalg[F].collectOn(perResource) ++
      Rule[AndE2Fwd].coalg[F].collectOn(perResource) ++
      Rule[ImpliesEFwd].coalg[F].collectOn(perResource)

  def alg[F: Form, T: Term.Aux[F]]: Algebra[[t] =>> NJ[F, t], T] =
    case r: ImpliesI[F, T]     => Rule[ImpliesI].alg[F, T].apply(r)
    case r: AndI[F, T]         => Rule[AndI].alg[F, T].apply(r)
    case r: OrI1[F, T]         => Rule[OrI1].alg[F, T].apply(r)
    case r: OrI2[F, T]         => Rule[OrI2].alg[F, T].apply(r)
    case r: TrueI[F, T]        => Rule[TrueI].alg[F, T].apply(r)
    case r: Ax[F, T]           => Rule[Ax].alg[F, T].apply(r)
    case r: FalseE[F, T]       => Rule[FalseE].alg[F, T].apply(r)
    case r: AndE1Back[F, T]    => Rule[AndE1Back].alg[F, T].apply(r)
    case r: AndE2Back[F, T]    => Rule[AndE2Back].alg[F, T].apply(r)
    case r: AndE1Fwd[F, T]     => Rule[AndE1Fwd].alg[F, T].apply(r)
    case r: AndE2Fwd[F, T]     => Rule[AndE2Fwd].alg[F, T].apply(r)
    case r: ImpliesEBack[F, T] => Rule[ImpliesEBack].alg[F, T].apply(r)
    case r: ImpliesEFwd[F, T]  => Rule[ImpliesEFwd].alg[F, T].apply(r)
    case r: OrE[F, T]          => Rule[OrE].alg[F, T].apply(r)

  def label[F: Form, T](c: NJ[F, T]): String = NJ.label(c)

  given traverse[F: Form]: Traverse[[t] =>> NJ[F, t]] = njTraverse[F]

  /** The sub-holes a move opens, in the order they are filled. */
  private def subgoals[F, T](rule: NJ[F, T]): List[T] = rule match
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

  private def njTraverse[F]: Traverse[[t] =>> NJ[F, t]] =
    new Traverse[[t] =>> NJ[F, t]]:

      def traverse[G[_]: Applicative, A, B](fa: NJ[F, A])(f: A => G[B]): G[NJ[F, B]] =
        fa match
          case ImpliesI(p, body) => f(body).map(ImpliesI(p, _))
          case AndI(fst, snd)    => (f(fst), f(snd)).mapN(AndI(_, _))
          case OrI1(arg, rt)     => f(arg).map(OrI1(_, rt))
          case OrI2(arg, lt)     => f(arg).map(OrI2(_, lt))
          case TrueI()           => Applicative[G].pure(TrueI())
          case Ax(v)             => Applicative[G].pure(Ax(v))
          case FalseE(v, g)      => Applicative[G].pure(FalseE(v, g))
          case AndE1Back(v)      => Applicative[G].pure(AndE1Back(v))
          case AndE2Back(v)      => Applicative[G].pure(AndE2Back(v))
          case AndE1Fwd(v, b, body) => f(body).map(AndE1Fwd(v, b, _))
          case AndE2Fwd(v, b, body) => f(body).map(AndE2Fwd(v, b, _))
          case ImpliesEBack(v, arg) => f(arg).map(ImpliesEBack(v, _))
          case ImpliesEFwd(v, arg, b, body) =>
            (f(arg), f(body)).mapN(ImpliesEFwd(v, _, b, _))
          case OrE(v, l, lb, r, rb) => (f(lb), f(rb)).mapN(OrE(v, l, _, r, _))

      def foldLeft[A, B](fa: NJ[F, A], b: B)(g: (B, A) => B): B =
        subgoals(fa).foldLeft(b)(g)

      def foldRight[A, B](fa: NJ[F, A], lb: Eval[B])(g: (A, Eval[B]) => Eval[B]): Eval[B] =
        Foldable[List].foldRight(subgoals(fa), lb)(g)
