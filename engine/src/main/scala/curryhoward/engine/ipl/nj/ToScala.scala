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

  /** The same pieces, laid out as idiomatic Scala over several lines.
    *
    * Two things decide the layout, and both come from the language rather than
    * from taste:
    *
    *   - **A `val` is a statement.** It may be written where statements go —
    *     the body of a lambda, of another `val`, of a `case` — and nowhere
    *     else. A `let` that lands in an *expression* position, which happens
    *     the moment a player makes a forward move inside `Left(…)`, is
    *     therefore wrapped in a block. Without that the render is not Scala at
    *     all: `Right(val p = pqr._1; (pqr._1, r))` does not compile.
    *   - **A `match` gets braces and a line per case**, and a case whose body
    *     is itself a block continues on the next line, indented — which is how
    *     the language is written and, conveniently, how a player reads one
    *     branch at a time.
    *
    * Kept apart from [[pieces]] rather than replacing it: the single-line form
    * is what the tests assert against, and what a tooltip wants.
    */
  def formatted(t: Lambda, ascribe: Boolean = true): List[Piece] =
    piecesFrom(t, Env.empty, ascribe, breaks = true)

  private def piecesFrom(
      t0: Lambda,
      env0: Env,
      ascribe: Boolean,
      breaks: Boolean = false
  ): List[Piece] =
    var next = 0

    /** `stmt` is whether statements may be written here — see above. It is
      * meaningless on the single-line path, where nothing breaks.
      */
    def go(t: Lambda, env: Env, depth: Int = 0, stmt: Boolean = true): List[Piece] =
      def text(s: String) = List(Piece.Text(s))
      def ann(ty: Formula) = if ascribe then s": ${tpe(ty)}" else ""
      def nl(at: Int) = "\n" + "  " * at

      /** Is this a block — something that must occupy lines of its own? */
      def isBlock(x: Lambda): Boolean = x match
        case Let(_, _, _)               => true
        case Match(_, _, _, _, _)       => true
        case _                          => false

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
          if !breaks then text(s"$binder => ") ++ go(body, inner)
          else text(s"$binder =>" + nl(depth + 1)) ++ go(body, inner, depth + 1, stmt = true)

        case App(f, arg) =>
          go(f, env, depth, stmt = false) ++ text("(") ++ go(arg, env, depth, stmt = false) ++ text(")")
        case Pair(a, b) =>
          text("(") ++ go(a, env, depth, stmt = false) ++ text(", ") ++
            go(b, env, depth, stmt = false) ++ text(")")
        case Fst(inner)       => go(inner, env, depth, stmt = false) ++ text("._1")
        case Snd(inner)       => go(inner, env, depth, stmt = false) ++ text("._2")
        case InL(inner, _)    => wrap("Left", inner, env, depth)
        case InR(inner, _)    => wrap("Right", inner, env, depth)
        case Absurd(inner, _) => go(inner, env, depth, stmt = false) ++ text(" match {}")

        case Let(binder, value, body) =>
          val (inner, n) = env.bind(binder)
          val binding =
            text(s"val $n${ann(binder._2)} = ") ++ go(value, env, depth, stmt = false)
          if !breaks then binding ++ text("; ") ++ go(body, inner)
          else if stmt then binding ++ text(nl(depth)) ++ go(body, inner, depth, stmt = true)
          else
            // An expression position: `val` needs a block around it.
            text("{" + nl(depth + 1)) ++
              piecesFrom(binder, value, body, env, depth + 1) ++
              text(nl(depth) + "}")

        case Match(scrutinee, left, onLeft, right, onRight) =>
          val (lEnv, ln) = env.bind(left)
          val (rEnv, rn) = env.bind(right)
          if !breaks then
            go(scrutinee, env) ++
              text(s" match { case Left($ln${ann(left._2)}) => ") ++ go(onLeft, lEnv) ++
              text(s"; case Right($rn${ann(right._2)}) => ") ++ go(onRight, rEnv) ++
              text(" }")
          else
            // A case whose body is a block starts on the next line; a simple
            // one stays where the eye already is.
            def branch(name: String, bound: String, ty: Formula, body: Lambda, env: Env) =
              val head = s"case $name($bound${ann(ty)}) =>"
              if isBlock(body) then
                text(head + nl(depth + 2)) ++ go(body, env, depth + 2, stmt = true)
              else text(head + " ") ++ go(body, env, depth + 1, stmt = false)

            go(scrutinee, env, depth, stmt = false) ++
              text(" match {" + nl(depth + 1)) ++
              branch("Left", ln, left._2, onLeft, lEnv) ++
              text(nl(depth + 1)) ++
              branch("Right", rn, right._2, onRight, rEnv) ++
              text(nl(depth) + "}")

    /** The inside of a block: the binding, then the body, at one indent. */
    def piecesFrom(
        binder: (Int, Formula),
        value: Lambda,
        body: Lambda,
        env: Env,
        depth: Int
    ): List[Piece] =
      val (inner, n) = env.bind(binder)
      val ann = if ascribe then s": ${tpe(binder._2)}" else ""
      List(Piece.Text(s"val $n$ann = ")) ++ go(value, env, depth, stmt = false) ++
        List(Piece.Text("\n" + "  " * depth)) ++ go(body, inner, depth, stmt = true)

    /** `Left(…)` / `Right(…)`, with the argument on its own lines when it is a
      * block — `Right {` … `}`, which is how the language is written.
      */
    def wrap(ctor: String, inner: Lambda, env: Env, depth: Int): List[Piece] =
      val block = breaks && (inner match
        case Let(_, _, _)         => true
        case Match(_, _, _, _, _) => true
        case _                    => false)
      if block then
        List(Piece.Text(s"$ctor {" + "\n" + "  " * (depth + 1))) ++
          go(inner, env, depth + 1, stmt = true) ++
          List(Piece.Text("\n" + "  " * depth + "}"))
      else List(Piece.Text(s"$ctor(")) ++ go(inner, env, depth, stmt = false) ++ List(Piece.Text(")"))

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
