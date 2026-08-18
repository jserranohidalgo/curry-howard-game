package curryhoward.engine
package ipl
package nj

import Partial.*
import Figure.*

/** The logician's reading of a position: a derivation, and the facts derived
  * beside it (D25).
  *
  * The rule that shapes everything here: **a `let` is not a rule of natural
  * deduction, so it never appears in the figure.** Instead its value becomes a
  * *derived fact* standing beside the main derivation, and every use of the
  * variable it binds is drawn as that fact's derivation, grafted in place.
  * Grafting is inlining, performed at the moment of use rather than at the end
  * — which is what lets a forward move show progress while its body is still a
  * hole, and what makes the finished figure the normal derivation of §4.9 with
  * nothing left to normalise.
  *
  * Three consequences, all of them correct rather than regrettable:
  *
  *   - a fact used twice is **drawn twice**, since a derivation is a tree and
  *     sharing belongs to the program;
  *   - a fact never used stays on the shelf and vanishes when the game is won,
  *     exactly as [[Cleanup]] drops a `let` whose variable is unused;
  *   - a hole inside a fact that is used twice is **drawn twice**, and both
  *     drawings are the same hole.
  *
  * This is a direct recursion rather than [[Partial.fold]], for the same reason
  * `fill` is: it threads an environment down (which fragment each let-bound
  * variable stands for) and a counter along (which hole is which), and a
  * catamorphism would have to carry both in its carrier to no one's benefit.
  */
object ToFigure:

  /** Where a resource's derivation comes from: either it is a hypothesis in
    * force, or it is a fact some forward move derived.
    */
  private type Env = Map[Int, Figure]

  private final case class St(
      nextHole: Int,
      nextLabel: Int,
      labels: Map[Int, Int],
      derived: Vector[Derived]
  ):
    /** Labels are handed out as the figure is read downwards, so `→I` at the
      * bottom carries ¹ and the case hypotheses of a `∨E` above it carry ² and
      * ³ — the numbering of the specification's own figure.
      */
    def label(v: Int): (Int, St) =
      labels.get(v) match
        case Some(n) => (n, this)
        case None    => (nextLabel, copy(nextLabel = nextLabel + 1, labels = labels + (v -> nextLabel)))

    def graft(binder: Int): St =
      copy(derived = derived.map(d => if d.binder == binder then d.copy(uses = d.uses + 1) else d))

  def apply(position: Partial): Forest =
    val (figure, st) = go(position, Map.empty, St(0, 1, Map.empty, Vector.empty))
    Forest(figure, st.derived.toList)

  /** The finished proof, when there is nothing left to fill: the same function,
    * and by then the shelf holds only facts nobody used.
    */
  def complete(position: Partial): Option[Forest] =
    Option.when(position.isComplete)(apply(position))

  private def go(p: Partial, env: Env, st: St): (Figure, St) = p match
    // An open leaf carries the hypotheses in force at it — `[A]ⁿ [B]ᵐ ⋮ C`,
    // which is §3.1's own notation for a subderivation not yet written out.
    // Without them a discharge label on a rule below points at nothing the
    // player can see, and the moment a branch grows any structure the
    // hypothesis would vanish from the figure entirely. Once the branch is
    // finished there are no open leaves left, so the annotations disappear and
    // what remains is the plain figure, with each hypothesis drawn where it is
    // used.
    case Open(hole) =>
      val (held, st1) = hypotheses(hole, env, st)
      (Todo(st1.nextHole, hole.con, held), st1.copy(nextHole = st1.nextHole + 1))

    case Node(rule) =>
      rule match
        // --- resources: a hypothesis in force, or a fact already derived ------
        case NJ.Ax((v, f)) => resource(v, f, env, st)

        case NJ.FalseE((v, f), goal) =>
          val (premise, st1) = resource(v, f, env, st)
          (Infer("⊥E", List(premise), goal, Nil), st1)

        case NJ.AndE1Back((v, f @ Formula.And(a, _))) =>
          val (premise, st1) = resource(v, f, env, st)
          (Infer("∧E₁", List(premise), a, Nil), st1)

        case NJ.AndE2Back((v, f @ Formula.And(_, b))) =>
          val (premise, st1) = resource(v, f, env, st)
          (Infer("∧E₂", List(premise), b, Nil), st1)

        case NJ.ImpliesEBack((v, f @ Formula.Implies(_, b)), arg) =>
          // The argument is the subgoal, so it is numbered first; the major
          // premise stands to its left in the drawing, as §3.1 has it.
          val (argFig, st1) = go(arg, env, st)
          val (major, st2) = resource(v, f, env, st1)
          (Infer("→E", List(major, argFig), b, Nil), st2)

        case NJ.OrE((v, f @ Formula.Or(a, b)), (l, _), onLeft, (r, _), onRight) =>
          // Labels first: an empty branch is drawn as `[A]ⁿ ⋮ C`, so the case
          // hypotheses have to be known before the branches are rendered.
          val (leftLabel, st1) = st.label(l)
          val (rightLabel, st2) = st1.label(r)
          val (leftFig, st3) = go(onLeft, env, st2)
          val (rightFig, st4) = go(onRight, env, st3)
          val (major, st5) = resource(v, f, env, st4)
          (
            Infer(
              "∨E",
              List(major, leftFig, rightFig),
              leftFig.conclusion,
              List(leftLabel, rightLabel)
            ),
            st5
          )

        // --- introductions ----------------------------------------------------
        case NJ.ImpliesI((v, a), body) =>
          val (label, st1) = st.label(v)
          val (bodyFig, st2) = go(body, env, st1)
          (
            Infer("→I", List(bodyFig), Formula.Implies(a, bodyFig.conclusion), List(label)),
            st2
          )

        case NJ.AndI(fst, snd) =>
          val (l, st1) = go(fst, env, st)
          val (r, st2) = go(snd, env, st1)
          (Infer("∧I", List(l, r), Formula.And(l.conclusion, r.conclusion), Nil), st2)

        case NJ.OrI1(arg, rightType) =>
          val (a, st1) = go(arg, env, st)
          (Infer("∨I₁", List(a), Formula.Or(a.conclusion, rightType), Nil), st1)

        case NJ.OrI2(arg, leftType) =>
          val (a, st1) = go(arg, env, st)
          (Infer("∨I₂", List(a), Formula.Or(leftType, a.conclusion), Nil), st1)

        case NJ.TrueI() => (Infer("⊤I", Nil, Formula.True, Nil), st)

        // --- the forward move: a fact beside the derivation, not a bar in it ---
        case NJ.Let((x, ty), value, body) =>
          val (fragment, st1) = go(value, env, st)
          val shelved = st1.copy(derived = st1.derived :+ Derived(x, ty, fragment, uses = 0))
          go(body, env + (x -> fragment), shelved)

        case other => (Todo(-1, conclusionOf(other)), st) // unreachable: every rule is above

  /** What may be assumed at a hole: every resource in scope that is a
    * *hypothesis* — bound by `→I` or `∨E`, and discharged further down — in the
    * order they came into force.
    *
    * A `let`-bound resource is deliberately not here. It is not an assumption
    * but a fact with a derivation behind it, and it is shown as one, beside the
    * tree (D25).
    */
  private def hypotheses(hole: Sequent, env: Env, st: St): (List[(Int, Formula)], St) =
    hole.ant.reverse.foldLeft((List.empty[(Int, Formula)], st)) { case ((held, acc), (v, f)) =>
      if env.contains(v) then (held, acc)
      else
        val (label, next) = acc.label(v)
        (held :+ (label, f), next)
    }

  /** The derivation standing behind a resource.
    *
    * A λ- or case-bound variable is a hypothesis, drawn bracketed and labelled
    * and discharged further down. A `let`-bound one is a derived fact, and
    * using it grafts its whole fragment here.
    */
  private def resource(v: Int, f: Formula, env: Env, st: St): (Figure, St) =
    env.get(v) match
      case Some(fragment) => (fragment, st.graft(v))
      case None =>
        val (label, st1) = st.label(v)
        (Hyp(label, f), st1)

  private def conclusionOf[T](rule: NJ[T]): Formula = rule match
    case NJ.Ax((_, f))                          => f
    case NJ.FalseE(_, goal)                     => goal
    case NJ.AndE1Back((_, Formula.And(a, _)))   => a
    case NJ.AndE2Back((_, Formula.And(_, b)))   => b
    case NJ.ImpliesEBack((_, Formula.Implies(_, b)), _) => b
    case _                                      => Formula.True
