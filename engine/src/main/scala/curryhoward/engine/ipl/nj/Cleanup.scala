package curryhoward.engine
package ipl
package nj

import Lambda.*

/** Dropping the scaffolding from a finished term — specification §4.9's "final
  * step".
  *
  * During play every binding is annotated and every forward step is a `val`,
  * because that is the work being shown. A finished program keeps one type
  * annotation, its own, and lets the rest be inferred. [[ToScala.bare]] drops
  * the annotations; this drops the bindings that only existed to name an
  * intermediate step.
  *
  * **A `let` is inlined only when its variable is used exactly once.** Used
  * twice, inlining would duplicate the value — sharing is precisely what a
  * binding buys — and used never, it can simply go. That is the same
  * restriction the logician's view runs into when it tries to inline a `let`
  * into a derivation (the open question under Phase 7), met here first and in
  * a place where the honest answer is cheap: keep the `val`.
  */
object Cleanup:

  def simplify(t: Lambda): Lambda = t match
    case Let(binder @ (v, _), value, body) =>
      val cleanValue = simplify(value)
      val cleanBody = simplify(body)
      occurrences(v, cleanBody) match
        case 0 => cleanBody // never used: the binding was a dead end
        case 1 => substitute(v, cleanValue, cleanBody)
        case _ => Let(binder, cleanValue, cleanBody) // used twice: sharing earns its keep

    case Lam(p, body)     => Lam(p, simplify(body))
    case App(f, arg)      => App(simplify(f), simplify(arg))
    case Pair(a, b)       => Pair(simplify(a), simplify(b))
    case Fst(inner)       => Fst(simplify(inner))
    case Snd(inner)       => Snd(simplify(inner))
    case InL(inner, ty)   => InL(simplify(inner), ty)
    case InR(inner, ty)   => InR(simplify(inner), ty)
    case Absurd(inner, g) => Absurd(simplify(inner), g)
    case Match(s, l, lb, r, rb) =>
      Match(simplify(s), l, simplify(lb), r, simplify(rb))
    case leaf @ (Var(_) | Unit | Hole(_)) => leaf

  def occurrences(v: Int, t: Lambda): Int = t match
    case Var(`v`)               => 1
    case Var(_) | Unit | Hole(_) => 0
    case Lam((w, _), body)      => if w == v then 0 else occurrences(v, body)
    case App(f, arg)            => occurrences(v, f) + occurrences(v, arg)
    case Pair(a, b)             => occurrences(v, a) + occurrences(v, b)
    case Fst(inner)             => occurrences(v, inner)
    case Snd(inner)             => occurrences(v, inner)
    case InL(inner, _)          => occurrences(v, inner)
    case InR(inner, _)          => occurrences(v, inner)
    case Absurd(inner, _)       => occurrences(v, inner)
    case Let((w, _), value, body) =>
      occurrences(v, value) + (if w == v then 0 else occurrences(v, body))
    case Match(s, (lv, _), lb, (rv, _), rb) =>
      occurrences(v, s) +
        (if lv == v then 0 else occurrences(v, lb)) +
        (if rv == v then 0 else occurrences(v, rb))

  /** Capture cannot happen: every binder the engine creates takes a fresh
    * number from `Sequent.nextVar`, so no two binders in a term share one and
    * a substituted term can never be captured by an enclosing binder.
    */
  private def substitute(v: Int, by: Lambda, t: Lambda): Lambda = t match
    case Var(`v`)                => by
    case leaf @ (Var(_) | Unit | Hole(_)) => leaf
    case Lam(p, body)            => if p._1 == v then t else Lam(p, substitute(v, by, body))
    case App(f, arg)             => App(substitute(v, by, f), substitute(v, by, arg))
    case Pair(a, b)              => Pair(substitute(v, by, a), substitute(v, by, b))
    case Fst(inner)              => Fst(substitute(v, by, inner))
    case Snd(inner)              => Snd(substitute(v, by, inner))
    case InL(inner, ty)          => InL(substitute(v, by, inner), ty)
    case InR(inner, ty)          => InR(substitute(v, by, inner), ty)
    case Absurd(inner, g)        => Absurd(substitute(v, by, inner), g)
    case Let(b, value, body) =>
      Let(b, substitute(v, by, value), if b._1 == v then body else substitute(v, by, body))
    case Match(s, l, lb, r, rb) =>
      Match(
        substitute(v, by, s),
        l,
        if l._1 == v then lb else substitute(v, by, lb),
        r,
        if r._1 == v then rb else substitute(v, by, rb)
      )
