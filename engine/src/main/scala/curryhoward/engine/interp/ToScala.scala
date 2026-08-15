package curryhoward.engine
package interp

import form.{Formula, Notation}
import term.Lambda
import term.Lambda.*

/** The programmer's reading: a term rendered as Scala source.
  *
  * Two jobs beyond walking the tree.
  *
  * **Names.** The engine numbers variables; this is where they get names, read
  * off their types after the prototype's `freshName` — `x` for a product, `qr`
  * for an `Either[Q, R]`, `q` for a `Q`. A player reading
  * `val qr: Either[Q, R] = pqr._2` learns something that `val v3 = v1._2` does
  * not teach.
  *
  * **Putting the sugar back.** The calculus has no `let`: a forward move builds
  * `(λx: A. body) value`. Rendering that literally would be honest and
  * unreadable, so the redex is recognised and printed as `val x: A = value;
  * body`. That is not a cheat — it is what Scala's `val` desugars to, read in
  * the other direction.
  */
object ToScala:

  def apply(t: Lambda): String = render(t, Env.empty)

  /** Names in force at a point in the term. */
  private case class Env(byVar: Map[Int, String], used: Set[String]):
    def name(v: Int): String = byVar.getOrElse(v, s"v$v")

    def bind(p: (Int, Formula)): (Env, String) =
      val (v, ty) = p
      val chosen = fresh(base(ty), used)
      (Env(byVar + (v -> chosen), used + chosen), chosen)

  private object Env:
    val empty: Env = Env(Map.empty, Set.empty)

  private def tpe(f: Formula): String = Notation.programmer(f)

  private def render(t: Lambda, env: Env): String = t match

    case Var(v)      => env.name(v)
    case Unit        => "()"
    case Hole(goal)  => s"… : ${tpe(goal)}"

    // The `let` redex, sugared back. A forward move produces exactly this
    // shape and nothing else does, so the sugar cannot misfire on a term the
    // player built some other way.
    case App(Lam((v, ty), body), value) =>
      val (inner, name) = env.bind((v, ty))
      s"val $name: ${tpe(ty)} = ${render(value, env)}; ${render(body, inner)}"

    case Lam((v, ty), body) =>
      val (inner, name) = env.bind((v, ty))
      s"($name: ${tpe(ty)}) => ${render(body, inner)}"

    case App(f, arg)    => s"${render(f, env)}(${render(arg, env)})"
    case Pair(a, b)     => s"(${render(a, env)}, ${render(b, env)})"
    case Fst(inner)     => s"${render(inner, env)}._1"
    case Snd(inner)     => s"${render(inner, env)}._2"
    case InL(inner, _)  => s"Left(${render(inner, env)})"
    case InR(inner, _)  => s"Right(${render(inner, env)})"
    case Absurd(inner, _) => s"${render(inner, env)} match {}"

    case Match(scrutinee, left, onLeft, right, onRight) =>
      val (lEnv, ln) = env.bind(left)
      val (rEnv, rn) = env.bind(right)
      s"${render(scrutinee, env)} match { " +
        s"case Left($ln: ${tpe(left._2)}) => ${render(onLeft, lEnv)}; " +
        s"case Right($rn: ${tpe(right._2)}) => ${render(onRight, rEnv)} }"

  // --- Naming ---------------------------------------------------------------

  private def base(ty: Formula): String = ty match
    case Formula.Atom(name)    => name.toLowerCase
    case Formula.False         => "z"
    case Formula.True          => "u"
    case Formula.Implies(_, _) => "f"
    case _ =>
      val atoms = leafAtoms(ty).map(_.toLowerCase)
      if atoms.nonEmpty && atoms.sizeIs <= 3 then atoms.mkString else "x"

  private def leafAtoms(ty: Formula): List[String] = ty match
    case Formula.Atom(n)       => List(n)
    case Formula.And(a, b)     => leafAtoms(a) ++ leafAtoms(b)
    case Formula.Or(a, b)      => leafAtoms(a) ++ leafAtoms(b)
    case Formula.Implies(a, b) => leafAtoms(a) ++ leafAtoms(b)
    case _                     => Nil

  private def fresh(base: String, used: Set[String]): String =
    if !used(base) then base
    else LazyList.from(2).map(i => s"$base$i").find(!used(_)).get
