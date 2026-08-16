package curryhoward.engine
package ipl
package nj

import Lambda.*

/** The programmer's reading: a term rendered as Scala source.
  *
  * Renders [[Lambda]] rather than folding `NJ` directly, so that one renderer
  * serves both a position mid-game (holes and all, via `ToLambda.position`) and
  * a finished term that has been through [[Cleanup]]. The naming lives here
  * too, and there is only one copy of it.
  *
  * Names are read off types, after the prototype's `freshName`: `x` for a
  * product, `qr` for an `Either[Q, R]`, `q` for a `Q`. A player reading
  * `val qr: Either[Q, R] = pqr._2` learns something that `val v3 = v1._2` does
  * not teach.
  */
object ToScala:

  /** As built: every binder carries its type, the way the game shows the work. */
  def apply(t: Lambda): String = render(t, Env.empty, ascribe = true)

  /** Cleaned up: the same term with the scaffolding dropped. §4.9's final step
    * — one type annotation for the whole program, everything else inferred.
    */
  def bare(t: Lambda): String = render(t, Env.empty, ascribe = false)

  /** The names a hole's scope carries, in binding order.
    *
    * A hole's antecedent holds exactly the binders enclosing it, innermost
    * first, so replaying the naming over its reverse reproduces the names the
    * whole-term render assigns. That is what lets a *fragment* — the effect of
    * a move, shown before it is played — speak of `pqr` rather than `v0`.
    */
  def names(scope: List[(Int, Formula)]): Map[Int, String] =
    scope.foldLeft(Env.empty)((env, binder) => env.bind(binder)._1).byVar

  /** Render a fragment against names already in force. */
  def fragment(t: Lambda, known: Map[Int, String]): String =
    render(t, Env(known, known.values.toSet), ascribe = true)

  private case class Env(byVar: Map[Int, String], used: Set[String]):
    def name(v: Int): String = byVar.getOrElse(v, s"v$v")

    def bind(p: (Int, Formula)): (Env, String) =
      val (v, ty) = p
      val chosen = fresh(base(ty), used)
      (Env(byVar + (v -> chosen), used + chosen), chosen)

  private object Env:
    val empty: Env = Env(Map.empty, Set.empty)

  private def tpe(f: Formula): String = Notation.programmer(f)

  private def render(t: Lambda, env: Env, ascribe: Boolean): String =
    def go(t: Lambda, env: Env): String = render(t, env, ascribe)
    def ann(ty: Formula): String = if ascribe then s": ${tpe(ty)}" else ""

    t match
      case Var(v)     => env.name(v)
      case Unit       => "()"
      case Hole(goal) => s"… : ${tpe(goal)}"

      case Lam(param, body) =>
        val (inner, n) = env.bind(param)
        // Unascribed, a single parameter needs no parentheses — `a => a`, which
        // is what §4.9's cleaned-up program writes.
        val binder = if ascribe then s"($n: ${tpe(param._2)})" else n
        s"$binder => ${go(body, inner)}"

      case App(f, arg)      => s"${go(f, env)}(${go(arg, env)})"
      case Pair(a, b)       => s"(${go(a, env)}, ${go(b, env)})"
      case Fst(inner)       => s"${go(inner, env)}._1"
      case Snd(inner)       => s"${go(inner, env)}._2"
      case InL(inner, _)    => s"Left(${go(inner, env)})"
      case InR(inner, _)    => s"Right(${go(inner, env)})"
      case Absurd(inner, _) => s"${go(inner, env)} match {}"

      case Let(binder, value, body) =>
        // The value is built outside the binding, so it renders in the outer
        // environment; the body sees the new name.
        val (inner, n) = env.bind(binder)
        s"val $n${ann(binder._2)} = ${go(value, env)}; ${go(body, inner)}"

      case Match(scrutinee, left, onLeft, right, onRight) =>
        val (lEnv, ln) = env.bind(left)
        val (rEnv, rn) = env.bind(right)
        s"${go(scrutinee, env)} match { " +
          s"case Left($ln${ann(left._2)}) => ${go(onLeft, lEnv)}; " +
          s"case Right($rn${ann(right._2)}) => ${go(onRight, rEnv)} }"

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
