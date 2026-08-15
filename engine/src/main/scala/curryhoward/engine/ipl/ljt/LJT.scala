package curryhoward.engine
package ipl
package ljt

import cats.{Applicative, Eval, Foldable, Traverse}
import cats.syntax.all.*
import Formula.{And, Or, Implies, Atom}
import Sequent.Prem

/** **LJT** — Dyckhoff's contraction-free sequent calculus for intuitionistic
  * propositional logic (also called G4ip).
  *
  * Its job here is to *decide*: given a goal, is it provable at all? That is
  * what Phase 6 needs to cross-check the game's own exhaustion against, and
  * what the deferred negative ending will eventually rest on. It is not played
  * — the game plays [[nj.NJ]] (D22) — so there is no game tree over it and no
  * term assignment yet. Herbelin's λ̄ arrives when LJ becomes a game of its own.
  *
  * Why this calculus rather than the game's own. Two properties natural
  * deduction does not have:
  *
  *   - **The subformula property.** Every formula in a cut-free proof is a
  *     subformula of the end-sequent, so nothing is ever invented. Left rules
  *     decompose what is already in the antecedent; right rules decompose the
  *     goal. Compare the game's `let`, where the bound type has to be guessed
  *     and the engine falls back on a heuristic.
  *   - **Termination.** Every rule *replaces* a formula with its parts, so a
  *     measure strictly decreases and backward search halts. The game's forward
  *     moves *accumulate* — the resource stays and a binding is added — which
  *     is precisely why its search space is unbounded.
  *
  * The interesting rules are the four for implication on the left. `LJ`'s
  * single `→L` may need the same premise twice (it is not contraction-free) and
  * so does not terminate; Dyckhoff splits it by the shape of the implication's
  * antecedent, and each case strictly decreases the measure. The termination
  * proof is his, not re-derived here (Dyckhoff 1992, *Contraction-free sequent
  * calculi for intuitionistic logic*).
  */
enum LJT[T]:

  // Axioms and constants.
  case Ax(v: Prem)
  case FalseL(v: Prem)
  case TrueR()

  // Right rules — decompose the goal.
  case AndR(fst: T, snd: T)
  case OrR1(arg: T)
  case OrR2(arg: T)
  case ImpliesR(param: Prem, body: T)

  // Left rules — decompose a resource.
  case AndL(v: Prem, first: Prem, second: Prem, body: T)
  case OrL(v: Prem, left: Prem, onLeft: T, right: Prem, onRight: T)

  /** `p → B` with `p` atomic and already in scope: keep `B`, drop the
    * implication.
    */
  case ImpliesLAtom(v: Prem, source: Prem, bound: Prem, body: T)

  /** `(A ∧ B) → C` becomes `A → (B → C)`. */
  case ImpliesLAnd(v: Prem, bound: Prem, body: T)

  /** `(A ∨ B) → C` becomes `A → C` and `B → C`. */
  case ImpliesLOr(v: Prem, leftBound: Prem, rightBound: Prem, body: T)

  /** `(A → B) → C`: prove `A → B` with `B → C` available, then continue with
    * `C`. The rule that makes the calculus contraction-free.
    */
  case ImpliesLImplies(v: Prem, assumed: Prem, minor: T, bound: Prem, major: T)

  /** `⊤ → C` becomes `C`; `⊥ → C` is useless and is discarded. */
  case ImpliesLTrue(v: Prem, bound: Prem, body: T)
  case ImpliesLFalse(v: Prem, body: T)

object LJT:

  type Interp[T] = LJT[T] => T

  private type Rule = PartialFunction[Sequent, LJT[Sequent]]

  /** Every rule applicable to this sequent.
    *
    * Right rules see the goal once; left rules see it once per resource, via
    * `Sequent.rotations`. Unlike the game's coalgebra this is a decision
    * procedure's move set, not a player's: it is complete, and it terminates.
    */
  def coalg(seq: Sequent): LazyList[LJT[Sequent]] =
    def fire(rules: List[Rule], on: LazyList[Sequent]): LazyList[LJT[Sequent]] =
      LazyList.from(rules).flatMap(rule => on.flatMap(rule.lift(_)))

    fire(axioms, seq.rotations) ++
      fire(rightRules, LazyList(seq)) ++
      fire(leftRules, seq.rotations)

  private val axioms: List[Rule] = List(
    { case Sequent((v, f) :: _, g) if f == g => Ax((v, f)) },
    { case Sequent((v, Formula.False) :: _, _) => FalseL((v, Formula.False)) }
  )

  private val rightRules: List[Rule] = List(
    { case Sequent(_, Formula.True) => TrueR() },
    { case Sequent(ant, And(a, b)) => AndR(Sequent(ant, a), Sequent(ant, b)) },
    { case Sequent(ant, Or(a, _)) => OrR1(Sequent(ant, a)) },
    { case Sequent(ant, Or(_, b)) => OrR2(Sequent(ant, b)) },
    { case seq @ Sequent(ant, Implies(a, b)) =>
      val x = (seq.nextVar, a)
      ImpliesR(x, Sequent(x :: ant, b))
    }
  )

  private val leftRules: List[Rule] = List(
    // A ∧ B is replaced by its components — the resource does *not* stay.
    { case seq @ Sequent((p @ (_, And(a, b))) :: gamma, g) =>
      val x = (seq.nextVar, a)
      val y = (seq.nextVar + 1, b)
      AndL(p, x, y, Sequent(x :: y :: gamma, g))
    },
    { case seq @ Sequent((p @ (_, Or(a, b))) :: gamma, g) =>
      val x = (seq.nextVar, a)
      val y = (seq.nextVar + 1, b)
      OrL(p, x, Sequent(x :: gamma, g), y, Sequent(y :: gamma, g))
    },
    // (⊥ → C) can never fire: discard it.
    { case Sequent((p @ (_, Implies(Formula.False, _))) :: gamma, g) =>
      ImpliesLFalse(p, Sequent(gamma, g))
    },
    // (⊤ → C) gives C outright.
    { case seq @ Sequent((p @ (_, Implies(Formula.True, c))) :: gamma, g) =>
      val x = (seq.nextVar, c)
      ImpliesLTrue(p, x, Sequent(x :: gamma, g))
    },
    // p → B with p atomic and already in scope: keep B. The guard is the
    // rule's side condition — without `p` there is nothing to fire it with.
    { case seq @ Sequent((p @ (_, Implies(a @ Atom(_), b))) :: gamma, g)
        if gamma.exists((_, ty) => ty == a) =>
      val source = gamma.find((_, ty) => ty == a).get
      val x = (seq.nextVar, b)
      ImpliesLAtom(p, source, x, Sequent(x :: gamma, g))
    },
    // (A ∧ B) → C  ⟹  A → (B → C)
    { case seq @ Sequent((p @ (_, Implies(And(a, b), c))) :: gamma, g) =>
      val x = (seq.nextVar, Implies(a, Implies(b, c)))
      ImpliesLAnd(p, x, Sequent(x :: gamma, g))
    },
    // (A ∨ B) → C  ⟹  A → C, B → C
    { case seq @ Sequent((p @ (_, Implies(Or(a, b), c))) :: gamma, g) =>
      val x = (seq.nextVar, Implies(a, c))
      val y = (seq.nextVar + 1, Implies(b, c))
      ImpliesLOr(p, x, y, Sequent(x :: y :: gamma, g))
    },
    // (A → B) → C  ⟹  prove A → B given B → C, then continue with C.
    { case seq @ Sequent((p @ (_, Implies(Implies(a, b), c))) :: gamma, g) =>
      val bc = (seq.nextVar, Implies(b, c))
      val x = (seq.nextVar + 1, c)
      ImpliesLImplies(
        p,
        bc,
        Sequent(bc :: gamma, Implies(a, b)),
        x,
        Sequent(x :: gamma, g)
      )
    }
  )

  def label[T](rule: LJT[T]): String = rule match
    case Ax(_)                          => "Ax"
    case FalseL(_)                      => "⊥L"
    case TrueR()                        => "⊤R"
    case AndR(_, _)                     => "∧R"
    case OrR1(_)                        => "∨R₁"
    case OrR2(_)                        => "∨R₂"
    case ImpliesR(_, _)                 => "→R"
    case AndL(_, _, _, _)               => "∧L"
    case OrL(_, _, _, _, _)             => "∨L"
    case ImpliesLAtom(_, _, _, _)       => "→L(atom)"
    case ImpliesLAnd(_, _, _)           => "→L(∧)"
    case ImpliesLOr(_, _, _, _)         => "→L(∨)"
    case ImpliesLImplies(_, _, _, _, _) => "→L(→)"
    case ImpliesLTrue(_, _, _)          => "→L(⊤)"
    case ImpliesLFalse(_, _)            => "→L(⊥)"

  def subgoals[T](rule: LJT[T]): List[T] = rule match
    case Ax(_)                                => Nil
    case FalseL(_)                            => Nil
    case TrueR()                              => Nil
    case AndR(fst, snd)                       => List(fst, snd)
    case OrR1(arg)                            => List(arg)
    case OrR2(arg)                            => List(arg)
    case ImpliesR(_, body)                    => List(body)
    case AndL(_, _, _, body)                  => List(body)
    case OrL(_, _, onLeft, _, onRight)        => List(onLeft, onRight)
    case ImpliesLAtom(_, _, _, body)          => List(body)
    case ImpliesLAnd(_, _, body)              => List(body)
    case ImpliesLOr(_, _, _, body)            => List(body)
    case ImpliesLImplies(_, _, minor, _, major) => List(minor, major)
    case ImpliesLTrue(_, _, body)             => List(body)
    case ImpliesLFalse(_, body)               => List(body)

  given Traverse[LJT] with

    def traverse[G[_]: Applicative, A, B](fa: LJT[A])(f: A => G[B]): G[LJT[B]] =
      fa match
        case Ax(v)                => Applicative[G].pure(Ax(v))
        case FalseL(v)            => Applicative[G].pure(FalseL(v))
        case TrueR()              => Applicative[G].pure(TrueR())
        case AndR(fst, snd)       => (f(fst), f(snd)).mapN(AndR(_, _))
        case OrR1(arg)            => f(arg).map(OrR1(_))
        case OrR2(arg)            => f(arg).map(OrR2(_))
        case ImpliesR(p, body)    => f(body).map(ImpliesR(p, _))
        case AndL(v, x, y, body)  => f(body).map(AndL(v, x, y, _))
        case OrL(v, l, lb, r, rb) => (f(lb), f(rb)).mapN(OrL(v, l, _, r, _))
        case ImpliesLAtom(v, s, b, body) => f(body).map(ImpliesLAtom(v, s, b, _))
        case ImpliesLAnd(v, b, body)     => f(body).map(ImpliesLAnd(v, b, _))
        case ImpliesLOr(v, l, r, body)   => f(body).map(ImpliesLOr(v, l, r, _))
        case ImpliesLImplies(v, a, minor, b, major) =>
          (f(minor), f(major)).mapN(ImpliesLImplies(v, a, _, b, _))
        case ImpliesLTrue(v, b, body) => f(body).map(ImpliesLTrue(v, b, _))
        case ImpliesLFalse(v, body)   => f(body).map(ImpliesLFalse(v, _))

    def foldLeft[A, B](fa: LJT[A], b: B)(g: (B, A) => B): B =
      subgoals(fa).foldLeft(b)(g)

    def foldRight[A, B](fa: LJT[A], lb: Eval[B])(g: (A, Eval[B]) => Eval[B]): Eval[B] =
      Foldable[List].foldRight(subgoals(fa), lb)(g)
