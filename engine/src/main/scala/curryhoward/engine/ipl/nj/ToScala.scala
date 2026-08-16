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

  /** A rendered term, in the pieces a user interface needs: the holes are
    * separable, because they are what the player clicks.
    */
  enum Piece:
    case Text(text: String)
    case HoleAt(index: Int, goal: Formula)

  /** As built: every binder carries its type, the way the game shows the work. */
  def apply(t: Lambda): String = plain(pieces(t))

  /** Cleaned up: the same term with the scaffolding dropped. §4.9's final step
    * — one type annotation for the whole program, everything else inferred.
    */
  def bare(t: Lambda): String = plain(pieces(t, ascribe = false))

  def plain(ps: List[Piece]): String = ps.map {
    case Piece.Text(t)          => t
    case Piece.HoleAt(_, goal)  => s"… : ${tpe(goal)}"
  }.mkString

  /** Holes are numbered left to right, which is the order `Partial.holes`
    * reports them in — so piece `i` is hole `i`, and a click knows which hole
    * it selected.
    */
  def pieces(t: Lambda, ascribe: Boolean = true): List[Piece] =
    piecesFrom(t, Env.empty, ascribe)

  private def piecesFrom(t0: Lambda, env0: Env, ascribe: Boolean): List[Piece] =
    var next = 0
    def go(t: Lambda, env: Env): List[Piece] =
      def text(s: String) = List(Piece.Text(s))
      def ann(ty: Formula) = if ascribe then s": ${tpe(ty)}" else ""
      t match
        case Hole(goal) =>
          val i = next
          next += 1
          List(Piece.HoleAt(i, goal))

        case Var(v) => text(env.name(v))
        case Unit   => text("()")

        case Lam(param, body) =>
          val (inner, n) = env.bind(param)
          val binder = if ascribe then s"($n: ${tpe(param._2)})" else n
          text(s"$binder => ") ++ go(body, inner)

        case App(f, arg)      => go(f, env) ++ text("(") ++ go(arg, env) ++ text(")")
        case Pair(a, b)       => text("(") ++ go(a, env) ++ text(", ") ++ go(b, env) ++ text(")")
        case Fst(inner)       => go(inner, env) ++ text("._1")
        case Snd(inner)       => go(inner, env) ++ text("._2")
        case InL(inner, _)    => text("Left(") ++ go(inner, env) ++ text(")")
        case InR(inner, _)    => text("Right(") ++ go(inner, env) ++ text(")")
        case Absurd(inner, _) => go(inner, env) ++ text(" match {}")

        case Let(binder, value, body) =>
          val (inner, n) = env.bind(binder)
          text(s"val $n${ann(binder._2)} = ") ++ go(value, env) ++ text("; ") ++ go(body, inner)

        case Match(scrutinee, left, onLeft, right, onRight) =>
          val (lEnv, ln) = env.bind(left)
          val (rEnv, rn) = env.bind(right)
          go(scrutinee, env) ++
            text(s" match { case Left($ln${ann(left._2)}) => ") ++ go(onLeft, lEnv) ++
            text(s"; case Right($rn${ann(right._2)}) => ") ++ go(onRight, rEnv) ++
            text(" }")

    go(t0, env0)

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
    plain(piecesFrom(t, Env(known, known.values.toSet), ascribe = true))

  private case class Env(byVar: Map[Int, String], used: Set[String]):
    def name(v: Int): String = byVar.getOrElse(v, s"v$v")

    def bind(p: (Int, Formula)): (Env, String) =
      val (v, ty) = p
      val chosen = fresh(base(ty), used)
      (Env(byVar + (v -> chosen), used + chosen), chosen)

  private object Env:
    val empty: Env = Env(Map.empty, Set.empty)

  private def tpe(f: Formula): String = Notation.programmer(f)

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
