package curryhoward.engine
package interp

import form.{Formula, Notation}
import calculus.{NJ, Sequent}
import NJ.*

/** The programmer's reading: a derivation rendered as Scala source.
  *
  * The carrier is `Env => String` rather than `String`, and it has to be. A
  * fold hands a rule its sub-terms *already interpreted*, but a binder must be
  * named before its body is rendered — `(pqr: (P, Either[Q, R])) => …` needs
  * `pqr` to exist for everything underneath it. Making the carrier a function
  * of the naming environment puts the binding back in the right order.
  *
  * Names are read off types, after the prototype's `freshName`: `x` for a
  * product, `qr` for an `Either[Q, R]`, `q` for a `Q`. A player reading
  * `val qr: Either[Q, R] = pqr._2` learns something that `val v3 = v1._2` does
  * not teach.
  */
object ToScala:

  /** Names in force at a point in the term. */
  final case class Env(byVar: Map[Int, String], used: Set[String]):
    def name(v: Int): String = byVar.getOrElse(v, s"v$v")

  object Env:
    val empty: Env = Env(Map.empty, Set.empty)

  type Render = Env => String

  def apply: NJ.Interp[Render] =

    def tpe(f: Formula): String = Notation.programmer(f)

    def bind(env: Env, p: Sequent.Prem): (Env, String) =
      val (v, ty) = p
      val chosen = fresh(base(ty), env.used)
      (Env(env.byVar + (v -> chosen), env.used + chosen), chosen)

    rule =>
      env =>
        rule match
          case ImpliesI(param, body) =>
            val (inner, n) = bind(env, param)
            s"($n: ${tpe(param._2)}) => ${body(inner)}"

          case AndI(fst, snd) => s"(${fst(env)}, ${snd(env)})"
          case OrI1(arg, _)   => s"Left(${arg(env)})"
          case OrI2(arg, _)   => s"Right(${arg(env)})"
          case TrueI()        => "()"

          case Ax((v, _))        => env.name(v)
          case FalseE((v, _), _) => s"${env.name(v)} match {}"
          case AndE1Back((v, _)) => s"${env.name(v)}._1"
          case AndE2Back((v, _)) => s"${env.name(v)}._2"

          case AndE1Fwd((v, _), bound, body) =>
            val (inner, n) = bind(env, bound)
            s"val $n: ${tpe(bound._2)} = ${env.name(v)}._1; ${body(inner)}"

          case AndE2Fwd((v, _), bound, body) =>
            val (inner, n) = bind(env, bound)
            s"val $n: ${tpe(bound._2)} = ${env.name(v)}._2; ${body(inner)}"

          case ImpliesEBack((v, _), arg) => s"${env.name(v)}(${arg(env)})"

          case ImpliesEFwd((v, _), arg, bound, body) =>
            // The argument is built outside the binding, so it renders in the
            // outer environment.
            val (inner, n) = bind(env, bound)
            s"val $n: ${tpe(bound._2)} = ${env.name(v)}(${arg(env)}); ${body(inner)}"

          case OrE((v, _), left, onLeft, right, onRight) =>
            val (lEnv, ln) = bind(env, left)
            val (rEnv, rn) = bind(env, right)
            s"${env.name(v)} match { " +
              s"case Left($ln: ${tpe(left._2)}) => ${onLeft(lEnv)}; " +
              s"case Right($rn: ${tpe(right._2)}) => ${onRight(rEnv)} }"

  /** An open hole, as the player sees it: its type, waiting. */
  def hole(h: Sequent): Render =
    _ => s"… : ${Notation.programmer(h.con)}"

  /** Render a whole derivation from the empty environment. */
  def show(render: Render): String = render(Env.empty)

  // --- Naming ---------------------------------------------------------------

  private def base(ty: Formula): String = ty match
    case Formula.Atom(name) => name.toLowerCase
    case Formula.False      => "z"
    case Formula.True       => "u"
    case Formula.Implies(_, _) => "f"
    case _ =>
      val atoms = leafAtoms(ty).map(_.toLowerCase)
      if atoms.nonEmpty && atoms.sizeIs <= 3 then atoms.mkString else "x"

  private def leafAtoms(ty: Formula): List[String] = ty match
    case Formula.Atom(n)       => List(n)
    case Formula.And(a, b)     => leafAtoms(a) ++ leafAtoms(b)
    case Formula.Or(a, b)      => leafAtoms(a) ++ leafAtoms(b)
    case Formula.Implies(a, b) => leafAtoms(a) ++ leafAtoms(b)
    case _                  => Nil

  private def fresh(base: String, used: Set[String]): String =
    if !used(base) then base
    else LazyList.from(2).map(i => s"$base$i").find(!used(_)).get
