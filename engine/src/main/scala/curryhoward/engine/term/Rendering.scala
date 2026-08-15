package curryhoward.engine
package term

import cats.Show
import form.{Form, Formula, Notation}
import form.Form.{Atom, And, Or, Implies}
import Lambda.*

/** Rendering a stored term as Scala source.
  *
  * The engine numbers variables; this is where they get names. The scheme
  * follows the prototype's `freshName`, which reads the name off the type — `x`
  * for a product, `qr` for an `Either[Q, R]`, `q` for a `Q` — because a player
  * reading `val qr: Either[Q, R] = pqr._2` learns something that `val v3 = v1._2`
  * does not teach.
  *
  * Phase 7 adds the second rendering, the natural-deduction derivation, as
  * another fold over the same term.
  */
object Rendering:

  given scalaShow: Show[Lambda] = Show.show(scala)

  def scala(t: Lambda): String = render(t, Names.empty)._1

  /** Names assigned so far, plus the set of names already taken. */
  private case class Names(byVar: Map[Int, String], used: Set[String]):
    def bind(v: Int, tpe: Formula): (Names, String) =
      val name = fresh(base(tpe), used)
      (Names(byVar + (v -> name), used + name), name)

    def apply(v: Int): String = byVar.getOrElse(v, s"v$v")

  private object Names:
    val empty: Names = Names(Map.empty, Set.empty)

  /** Base name suggested by a type, after the prototype's `freshName`. */
  private def base(tpe: Formula): String = tpe match
    case Atom(name)    => name.toLowerCase
    case Form.False()  => "z"
    case Form.True()   => "u"
    case Implies(_, _) => "f"
    case _ =>
      val atoms = leafAtoms(tpe).map(_.toLowerCase)
      if atoms.nonEmpty && atoms.sizeIs <= 3 then atoms.mkString else "x"

  private def leafAtoms(tpe: Formula): List[String] = tpe match
    case Atom(n)       => List(n)
    case And(a, b)     => leafAtoms(a) ++ leafAtoms(b)
    case Or(a, b)      => leafAtoms(a) ++ leafAtoms(b)
    case Implies(a, b) => leafAtoms(a) ++ leafAtoms(b)
    case _             => Nil

  private def fresh(base: String, used: Set[String]): String =
    if !used(base) then base
    else LazyList.from(2).map(i => s"$base$i").find(!used(_)).get

  private def tpe(f: Formula): String = Notation.programmer[Formula].show(f)

  /** Returns the rendered term and the names in force after it, so that sibling
    * subterms do not reuse a name bound in the other branch.
    */
  private def render(t: Lambda, names: Names): (String, Names) = t match

    case Var(v) => (names(v), names)
    case Unit   => ("()", names)

    case Lam((v, ty), body) =>
      val (n1, name) = names.bind(v, ty)
      val (b, n2) = render(body, n1)
      (s"($name: ${tpe(ty)}) => $b", n2)

    case App(f, arg) =>
      val (fs, n1) = render(f, names)
      val (as, n2) = render(arg, n1)
      (s"$fs($as)", n2)

    case Pair(a, b) =>
      val (as, n1) = render(a, names)
      val (bs, n2) = render(b, n1)
      (s"($as, $bs)", n2)

    case Fst(t)          => val (s, n) = render(t, names); (s"$s._1", n)
    case Snd(t)          => val (s, n) = render(t, names); (s"$s._2", n)
    case InL(t, _)       => val (s, n) = render(t, names); (s"Left($s)", n)
    case InR(t, _)       => val (s, n) = render(t, names); (s"Right($s)", n)
    case Absurd(t, _)    => val (s, n) = render(t, names); (s"$s match {}", n)

    case Match(scrutinee, (lv, lt), onLeft, (rv, rt), onRight) =>
      val (ss, n0) = render(scrutinee, names)
      val (n1, ln) = n0.bind(lv, lt)
      val (ls, n2) = render(onLeft, n1)
      val (n3, rn) = n2.bind(rv, rt)
      val (rs, n4) = render(onRight, n3)
      (
        s"$ss match { case Left($ln: ${tpe(lt)}) => $ls; case Right($rn: ${tpe(rt)}) => $rs }",
        n4
      )

    case Let((v, ty), value, body) =>
      val (vs, n0) = render(value, names)
      val (n1, name) = n0.bind(v, ty)
      val (bs, n2) = render(body, n1)
      (s"val $name: ${tpe(ty)} = $vs; $bs", n2)
