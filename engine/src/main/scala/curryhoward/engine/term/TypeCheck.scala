package curryhoward.engine
package term

import form.{Formula, Notation}
import form.Formula.{And, Or, Implies}
import Lambda.*

/** An independent check that a term is well-typed.
  *
  * Deliberately *not* built from the rules that produced the term. Move
  * generation and this checker agree only if both are right, which is what
  * makes it worth having: the tests run every term the engine builds through
  * here rather than trusting the calculus to be correct by construction.
  *
  * Terms carry exactly the annotations this needs — a lambda's parameter type,
  * the type not taken by `inl`/`inr`, a `let`'s binding, the goal of an
  * `absurd` — so inference never has to guess.
  */
object TypeCheck:

  final case class TypeError(message: String):
    override def toString: String = message

  type Ctx = List[(Int, Formula)]

  /** Infer the type of a closed-enough term, or say why it does not have one. */
  def infer(t: Lambda, ctx: Ctx = Nil): Either[TypeError, Formula] =

    def fail(msg: String) = Left(TypeError(msg))
    def show(f: Formula) = Notation.programmer(f)

    t match

      case Var(v) =>
        ctx.collectFirst { case (`v`, f) => f }
          .toRight(TypeError(s"variable $v is not in scope"))

      case Unit => Right(Formula.True)

      case Lam((v, ty), body) =>
        infer(body, (v, ty) :: ctx).map(ty ==> _)

      case App(f, arg) =>
        for
          ft <- infer(f, ctx)
          at <- infer(arg, ctx)
          res <- ft match
            case Implies(dom, cod) =>
              if dom == at then Right(cod)
              else fail(s"applied a ${show(ft)} to a ${show(at)}")
            case other => fail(s"${show(other)} is not a function")
        yield res

      case Pair(a, b) =>
        for at <- infer(a, ctx); bt <- infer(b, ctx) yield at /\ bt

      case Fst(inner) =>
        infer(inner, ctx).flatMap {
          case And(a, _) => Right(a)
          case other     => fail(s"._1 on a ${show(other)}, which is not a pair")
        }

      case Snd(inner) =>
        infer(inner, ctx).flatMap {
          case And(_, b) => Right(b)
          case other     => fail(s"._2 on a ${show(other)}, which is not a pair")
        }

      case InL(inner, rightType) => infer(inner, ctx).map(_ \/ rightType)
      case InR(inner, leftType)  => infer(inner, ctx).map(leftType \/ _)

      case Match(scrutinee, (lv, lt), onLeft, (rv, rt), onRight) =>
        infer(scrutinee, ctx).flatMap {
          case Or(a, _) if a != lt =>
            fail(s"left branch binds a ${show(lt)} but the scrutinee carries a ${show(a)}")
          case Or(_, b) if b != rt =>
            fail(s"right branch binds a ${show(rt)} but the scrutinee carries a ${show(b)}")
          case Or(_, _) =>
            for
              l <- infer(onLeft, (lv, lt) :: ctx)
              r <- infer(onRight, (rv, rt) :: ctx)
              res <-
                if l == r then Right(l)
                else fail(s"branches disagree: ${show(l)} vs ${show(r)}")
            yield res
          case other => fail(s"matched on a ${show(other)}, which is not a sum")
        }

      case Hole(goal) => fail(s"unfilled hole of type ${show(goal)}")

      case Absurd(inner, goal) =>
        infer(inner, ctx).flatMap { it =>
          if it == Formula.False then Right(goal)
          else fail(s"absurd on a ${show(it)}, which is not ⊥")
        }

  /** Does this term inhabit this type? */
  def check(t: Lambda, expected: Formula, ctx: Ctx = Nil): Either[TypeError, Unit] =
    infer(t, ctx).flatMap { actual =>
      if actual == expected then Right(())
      else
        Left(
          TypeError(
            s"expected ${Notation.programmer(expected)}, got ${Notation.programmer(actual)}"
          )
        )
    }
