package curryhoward.engine
package calculus
package nj

import form.Form
import form.Form.{And, Or, Implies}
import term.Term
import Sequent.Prem

/** Natural deduction — **the rules of the game**.
  *
  * One case per cell of specification §3.2. Constructors act on the goal;
  * destructors act on a resource in scope, and come in two flavours, which is
  * where this parts company with any sequent calculus:
  *
  *   - **backward**, filling the hole outright when the resource's shape
  *     already matches it (`x._1` closing a `… : P`);
  *   - **forward**, binding the result as a new resource and carrying on
  *     (`val qr: Either[Q, R] = x._2`), which is how an intermediate value — in
  *     particular a scrutinee for `∨E` — is brought into scope.
  *
  * The projections are four separate rules rather than one parameterised by a
  * side. Partly because the rules table shows `∧.E₁` and `∧.E₂` as distinct
  * cells and choosing between them is the lesson; partly because a
  * `PartialFunction` yields a single move, so folding them together would let
  * one silently shadow the other whenever both components match the goal.
  */
enum NJ[F, T]:

  // Constructors — build the goal's shape.
  case ImpliesI(param: Prem[F], body: T)
  case AndI(fst: T, snd: T)
  case OrI1(arg: T, rightType: F)
  case OrI2(arg: T, leftType: F)
  case TrueI()

  // Destructors — consume a resource in scope.
  case Ax(v: Prem[F])
  case FalseE(v: Prem[F], goal: F)
  case AndE1Back(v: Prem[F])
  case AndE2Back(v: Prem[F])
  case AndE1Fwd(v: Prem[F], bound: Prem[F], body: T)
  case AndE2Fwd(v: Prem[F], bound: Prem[F], body: T)
  case ImpliesEBack(v: Prem[F], arg: T)
  case ImpliesEFwd(v: Prem[F], arg: T, bound: Prem[F], body: T)
  case OrE(v: Prem[F], left: Prem[F], onLeft: T, right: Prem[F], onRight: T)

object NJ:

  type Aux[F] = [t] =>> NJ[F, t]

  given Calculus[NJ] = Calculus_NJ

  /** The name shown in the rules table and the search path. */
  def label[F, T](rule: NJ[F, T]): String = rule match
    case ImpliesI(_, _)          => "⟶.I"
    case AndI(_, _)              => "∧.I"
    case OrI1(_, _)              => "∨.I₁"
    case OrI2(_, _)              => "∨.I₂"
    case TrueI()                 => "⊤.I"
    case Ax(_)                   => "Ax"
    case FalseE(_, _)            => "⊥.E"
    case AndE1Back(_)            => "∧.E₁"
    case AndE2Back(_)            => "∧.E₂"
    case AndE1Fwd(_, _, _)       => "∧.E₁"
    case AndE2Fwd(_, _, _)       => "∧.E₂"
    case ImpliesEBack(_, _)      => "⟶.E"
    case ImpliesEFwd(_, _, _, _) => "⟶.E"
    case OrE(_, _, _, _, _)      => "∨.E"

  /** Whether the move opens holes, binds resources, or closes the goal outright
    * — what the Play screen needs to describe a move before it is taken.
    */
  def isForward[F, T](rule: NJ[F, T]): Boolean = rule match
    case AndE1Fwd(_, _, _) | AndE2Fwd(_, _, _) | ImpliesEFwd(_, _, _, _) => true
    case _                                                               => false

  // --- Constructors --------------------------------------------------------

  given Rule[ImpliesI] with
    def coalg[F: Form]: PartialFunction[Sequent[F], ImpliesI[F, Sequent[F]]] =
      case seq @ Sequent(ant, Implies(a, b)) =>
        val x = (seq.nextVar, a)
        ImpliesI(x, Sequent(x :: ant, b))

    def alg[F: Form, T: Term.Aux[F]]: ImpliesI[F, T] => T =
      case ImpliesI(param, body) => param.lam(body)

  given Rule[AndI] with
    def coalg[F: Form]: PartialFunction[Sequent[F], AndI[F, Sequent[F]]] =
      case Sequent(ant, And(a, b)) => AndI(Sequent(ant, a), Sequent(ant, b))

    def alg[F: Form, T: Term.Aux[F]]: AndI[F, T] => T =
      case AndI(fst, snd) => fst and snd

  given Rule[OrI1] with
    def coalg[F: Form]: PartialFunction[Sequent[F], OrI1[F, Sequent[F]]] =
      case Sequent(ant, Or(a, b)) => OrI1(Sequent(ant, a), b)

    def alg[F: Form, T: Term.Aux[F]]: OrI1[F, T] => T =
      case OrI1(arg, rightType) => arg.inl(rightType)

  given Rule[OrI2] with
    def coalg[F: Form]: PartialFunction[Sequent[F], OrI2[F, Sequent[F]]] =
      case Sequent(ant, Or(a, b)) => OrI2(Sequent(ant, b), a)

    def alg[F: Form, T: Term.Aux[F]]: OrI2[F, T] => T =
      case OrI2(arg, leftType) => arg.inr(leftType)

  given Rule[TrueI] with
    def coalg[F: Form]: PartialFunction[Sequent[F], TrueI[F, Sequent[F]]] =
      case Sequent(_, g) if Form.True.unapply(g) => TrueI()

    def alg[F: Form, T: Term.Aux[F]]: TrueI[F, T] => T =
      case TrueI() => Term[F, T].unit

  // --- Destructors ---------------------------------------------------------
  // These match on the *head* of the antecedent; `Sequent.rotations` presents
  // each resource at the head in turn, so one rule yields one move per
  // applicable resource — which is what the table's instance count shows.
  //
  // A consumed resource stays in scope: `x` is still available after
  // `val qr = x._2`, and §4.9 relies on that when it closes a hole with `x._1`
  // several moves later.

  given Rule[Ax] with
    def coalg[F: Form]: PartialFunction[Sequent[F], Ax[F, Sequent[F]]] =
      case Sequent((v, f) :: _, g) if Form.eqv(f, g) => Ax((v, f))

    def alg[F: Form, T: Term.Aux[F]]: Ax[F, T] => T =
      case Ax((v, _)) => v.`var`

  given Rule[FalseE] with
    def coalg[F: Form]: PartialFunction[Sequent[F], FalseE[F, Sequent[F]]] =
      case Sequent((v, f) :: _, g) if Form.False.unapply(f) => FalseE((v, f), g)

    def alg[F: Form, T: Term.Aux[F]]: FalseE[F, T] => T =
      case FalseE((v, _), goal) => v.`var`.absurd(goal)

  given Rule[AndE1Back] with
    def coalg[F: Form]: PartialFunction[Sequent[F], AndE1Back[F, Sequent[F]]] =
      case Sequent((v, f @ And(a, _)) :: _, g) if Form.eqv(a, g) => AndE1Back((v, f))

    def alg[F: Form, T: Term.Aux[F]]: AndE1Back[F, T] => T =
      case AndE1Back((v, _)) => v.`var`._1

  given Rule[AndE2Back] with
    def coalg[F: Form]: PartialFunction[Sequent[F], AndE2Back[F, Sequent[F]]] =
      case Sequent((v, f @ And(_, b)) :: _, g) if Form.eqv(b, g) => AndE2Back((v, f))

    def alg[F: Form, T: Term.Aux[F]]: AndE2Back[F, T] => T =
      case AndE2Back((v, _)) => v.`var`._2

  given Rule[AndE1Fwd] with
    def coalg[F: Form]: PartialFunction[Sequent[F], AndE1Fwd[F, Sequent[F]]] =
      case seq @ Sequent((p @ (_, And(a, _))) :: gamma, g) =>
        val bound = (seq.nextVar, a)
        AndE1Fwd(p, bound, Sequent(bound :: p :: gamma, g))

    def alg[F: Form, T: Term.Aux[F]]: AndE1Fwd[F, T] => T =
      case AndE1Fwd((v, _), bound, body) => Term[F, T].let(bound, v.`var`._1, body)

  given Rule[AndE2Fwd] with
    def coalg[F: Form]: PartialFunction[Sequent[F], AndE2Fwd[F, Sequent[F]]] =
      case seq @ Sequent((p @ (_, And(_, b))) :: gamma, g) =>
        val bound = (seq.nextVar, b)
        AndE2Fwd(p, bound, Sequent(bound :: p :: gamma, g))

    def alg[F: Form, T: Term.Aux[F]]: AndE2Fwd[F, T] => T =
      case AndE2Fwd((v, _), bound, body) => Term[F, T].let(bound, v.`var`._2, body)

  given Rule[ImpliesEBack] with
    def coalg[F: Form]: PartialFunction[Sequent[F], ImpliesEBack[F, Sequent[F]]] =
      case Sequent((p @ (_, Implies(a, b))) :: gamma, g) if Form.eqv(b, g) =>
        ImpliesEBack(p, Sequent(p :: gamma, a))

    def alg[F: Form, T: Term.Aux[F]]: ImpliesEBack[F, T] => T =
      case ImpliesEBack((v, _), arg) => v.`var`.apply(arg)

  given Rule[ImpliesEFwd] with
    def coalg[F: Form]: PartialFunction[Sequent[F], ImpliesEFwd[F, Sequent[F]]] =
      case seq @ Sequent((p @ (_, Implies(a, b))) :: gamma, g) =>
        val bound = (seq.nextVar, b)
        ImpliesEFwd(p, Sequent(p :: gamma, a), bound, Sequent(bound :: p :: gamma, g))

    def alg[F: Form, T: Term.Aux[F]]: ImpliesEFwd[F, T] => T =
      case ImpliesEFwd((v, _), arg, bound, body) =>
        Term[F, T].let(bound, v.`var`.apply(arg), body)

  given Rule[OrE] with
    def coalg[F: Form]: PartialFunction[Sequent[F], OrE[F, Sequent[F]]] =
      case seq @ Sequent((p @ (_, Or(a, b))) :: gamma, g) =>
        val l = (seq.nextVar, a)
        val r = (seq.nextVar + 1, b)
        OrE(p, l, Sequent(l :: p :: gamma, g), r, Sequent(r :: p :: gamma, g))

    def alg[F: Form, T: Term.Aux[F]]: OrE[F, T] => T =
      case OrE((v, _), left, onLeft, right, onRight) =>
        Term[F, T].matchOn(v.`var`, left, onLeft, right, onRight)
