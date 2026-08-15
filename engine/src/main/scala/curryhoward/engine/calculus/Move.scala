package curryhoward.engine
package calculus

import form.Formula
import form.Formula.{And, Or, Implies}
import Sequent.Prem
import Partial.{Open, Node}
import NJ.*

/** A **move**: what the player may do at a hole.
  *
  * A move is not a rule. It is a *skeleton* — a small piece of derivation with
  * its own holes — that gets dropped into the hole being played. Most skeletons
  * are one rule deep, but the forward destructors are three, because a forward
  * use is a `let`, and a `let` is an implication introduced and immediately
  * eliminated.
  *
  * Keeping the two apart is what lets [[NJ]] stay minimal and general while the
  * move set stays finite and enumerable. It is also the honest shape of the
  * thing: specification §3.2 is a table of *moves*, and §3.1 a table of rules,
  * and they were never in bijection — `∧.E₁` appears in §3.2 twice, backward
  * and forward.
  */
final case class Move(
    /** The rules-table cell this move belongs to. */
    label: String,
    /** A forward move binds its result as a new resource instead of filling
      * the hole with it.
      */
    forward: Boolean,
    /** The resource consumed, for a destructor. */
    actsOn: Option[Prem],
    /** The type this move binds as a new resource, for a forward move. */
    binds: Option[Formula],
    /** The derivation fragment this move installs, holes and all. */
    skeleton: Partial
)

object Move:

  /** Every move available at a hole, in the order the rules table lists them.
    *
    * Constructors see the goal once. Destructors see it once per resource in
    * scope — `Sequent.rotations` presents each resource at the head in turn, so
    * one rule yields one move per resource it applies to, which is what the
    * table's instance count shows.
    *
    * A consumed resource stays in scope: `x` is still available after
    * `val qr = x._2`, and §4.9 relies on that when it closes a hole with `x._1`
    * several moves later.
    */
  def at(seq: Sequent): LazyList[Move] =
    constructors(seq) ++ seq.rotations.flatMap(destructors)

  // --- Constructors: build the goal's shape ---------------------------------

  private def constructors(seq: Sequent): LazyList[Move] =
    val Sequent(ant, goal) = seq
    goal match
      case Implies(a, b) =>
        val x = (seq.nextVar, a)
        LazyList(Move("⟶.I", false, None, None, Node(ImpliesI(x, Open(Sequent(x :: ant, b))))))

      case And(a, b) =>
        LazyList(Move("∧.I", false, None, None, Node(AndI(Open(Sequent(ant, a)), Open(Sequent(ant, b))))))

      case Or(a, b) =>
        LazyList(
          Move("∨.I₁", false, None, None, Node(OrI1(Open(Sequent(ant, a)), b))),
          Move("∨.I₂", false, None, None, Node(OrI2(Open(Sequent(ant, b)), a)))
        )

      case Formula.True => LazyList(Move("⊤.I", false, None, None, Node(TrueI())))
      case _            => LazyList.empty

  // --- Destructors: consume the resource at the head of the antecedent ------

  private def destructors(seq: Sequent): LazyList[Move] =
    seq.ant match
      case Nil          => LazyList.empty
      case p :: gamma => backward(seq, p, gamma) ++ forward(seq, p, gamma)

  /** Moves that fill the hole outright with something extracted from `p`. */
  private def backward(seq: Sequent, p: Prem, gamma: List[Prem]): LazyList[Move] =
    val goal = seq.con
    val (_, ty) = p
    val use = Node(Ax(p)) // the resource itself, as a derivation

    val axiom =
      if ty == goal then LazyList(Move("Ax", false, Some(p), None, use)) else LazyList.empty

    val absurd =
      if ty == Formula.False then LazyList(Move("⊥.E", false, Some(p), None, Node(FalseE(use, goal))))
      else LazyList.empty

    val projections = ty match
      case And(a, b) =>
        (if a == goal then LazyList(Move("∧.E₁", false, Some(p), None, Node(AndE1(use))))
         else LazyList.empty) ++
          (if b == goal then LazyList(Move("∧.E₂", false, Some(p), None, Node(AndE2(use))))
           else LazyList.empty)
      case _ => LazyList.empty

    val application = ty match
      case Implies(a, b) if b == goal =>
        LazyList(Move("⟶.E", false, Some(p), None, Node(ImpliesE(use, Open(Sequent(p :: gamma, a))))))
      case _ => LazyList.empty

    val caseSplit = ty match
      case Or(a, b) =>
        val l = (seq.nextVar, a)
        val r = (seq.nextVar + 1, b)
        LazyList(
          Move(
            "∨.E",
            false,
            Some(p),
            None,
            Node(
              OrE(
                use,
                l,
                Open(Sequent(l :: p :: gamma, goal)),
                r,
                Open(Sequent(r :: p :: gamma, goal))
              )
            )
          )
        )
      case _ => LazyList.empty

    axiom ++ absurd ++ projections ++ application ++ caseSplit

  /** Moves that bind what they extract as a new resource and carry on.
    *
    * Each is a `let`, and a `let` is `(λx: A. body) value` — which is why these
    * skeletons are three rules deep while every other move is one. The
    * renderers put the sugar back: `interp.ToScala` prints this shape as
    * `val x: A = value; body`, which is what `val` *is* in Scala.
    */
  private def forward(seq: Sequent, p: Prem, gamma: List[Prem]): LazyList[Move] =
    val goal = seq.con
    val (_, ty) = p
    val use = Node(Ax(p))

    def let(label: String, boundType: Formula, value: Partial): Move =
      val x = (seq.nextVar, boundType)
      Move(
        label,
        forward = true,
        actsOn = Some(p),
        binds = Some(boundType),
        skeleton = Node(
          ImpliesE(
            Node(ImpliesI(x, Open(Sequent(x :: p :: gamma, goal)))),
            value
          )
        )
      )

    ty match
      case And(a, b) =>
        LazyList(
          let("∧.E₁", a, Node(AndE1(use))),
          let("∧.E₂", b, Node(AndE2(use)))
        )
      case Implies(a, b) =>
        LazyList(let("⟶.E", b, Node(ImpliesE(use, Open(Sequent(p :: gamma, a))))))
      case _ => LazyList.empty
