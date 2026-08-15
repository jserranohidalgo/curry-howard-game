package curryhoward.engine

import munit.FunSuite
import form.{Formula}
import form.Formula.Syntax.atom
import term.{Lambda, TypeCheck}
import interp.{ToScala, ToLambda}
import calculus.*

/** Playing the game, rather than searching it: positions, moves, the tree. */
class GameSuite extends FunSuite:


  val P = "P".atom
  val Q = "Q".atom
  val R = "R".atom
  val A = "A".atom
  val B = "B".atom

  val distributivity: Formula =
    (P /\ (Q \/ R)) ==> ((P /\ Q) \/ (P /\ R))

  /** Take the move with this label at the first hole whose moves offer it,
    * preferring forward or backward use as asked. Mirrors what a player does:
    * pick a cell in the rules table.
    */
  extension (tree: GameTree)
    def take(label: String, forward: Boolean = false): GameTree =
      val choice = tree.options.find { case (_, move) =>
        move.label == label && move.forward == forward
      }
      choice match
        case Some((key, _)) => tree.play(key.hole, key.moveIndex)
        case None =>
          fail(
            s"no $label (forward=$forward) available; offered: " +
              tree.options.map((_, m) => s"${m.label}${if m.forward then "→" else ""}").mkString(", ")
          )

  test("the opening position has one hole, the goal, and empty scope") {
    val g = GameTree.start(distributivity)
    val holes = g.current.position.holes
    assertEquals(holes.length, 1)
    val (path, hole) = holes.head
    assertEquals(path, Nil)
    assertEquals(hole.ant, Nil)
    assert(hole.con == distributivity)
    assertEquals(g.current.position.status, Status.Open)
  }

  test("§4.9 played move for move") {
    // The worked playthrough of the specification: the forced opening lambda,
    // a forward ∧.E₂ that brings the disjunction into scope, the case split,
    // then each branch built and closed.
    val played = GameTree
      .start(distributivity)
      .take("⟶.I")                      // move 1
      .take("∧.E₂", forward = true)     // move 2 — val qr = pqr._2
      .take("∨.E")                      // move 3 — case split
      .take("∨.I₁")                     // move 4 — left branch, informed choice
      .take("∧.I")                      // move 5
      .take("∧.E₁")                     // move 6 — backward: closes … : P
      .take("Ax")                       // move 7
      .take("∨.I₂")                     // move 8 — right branch
      .take("∧.I")                      // move 9
      .take("∧.E₁")                     // move 10
      .take("Ax")                       // move 11

    assertEquals(played.current.position.status, Status.Won)
    assertEquals(played.depth, 11, "the specification's playthrough is eleven moves")

    // The same finished position, read two ways.
    val term = ToLambda.complete(played.current.position).get
    val rendered = ToScala(term)
    assertEquals(
      rendered,
      "(pqr: (P, Either[Q, R])) => val qr: Either[Q, R] = pqr._2; " +
        "qr match { case Left(q: Q) => Left((pqr._1, q)); case Right(r: R) => Right((pqr._1, r)) }"
    )
    assertEquals(TypeCheck.check(term, distributivity), Right(()))
  }

  test("a hole with no move at all is an immediate dead end") {
    // §4.10, branch A of excluded middle: `Left(… : A)` with nothing in scope.
    // An atom has no constructor and there is no resource to destruct, so the
    // hole admits no move — the narrow sense of "dead end" the interaction
    // spec §6.2 uses, and the only one the engine decides on its own.
    val em = A \/ (A ==> Formula.False)
    val stuck = GameTree.start(em).take("∨.I₁")
    assertEquals(stuck.current.position.deadHoles.length, 1)
    assertEquals(stuck.current.position.status, Status.Dead)
  }

  test("the premature ∨.I of §4.9 is a losing line, not a stuck one") {
    // The distinction matters, and the specification blurs it: committing to
    // Left before inspecting the disjunction does *not* strand the player
    // immediately — `val qr = pqr._2` and a case split are both still legal.
    // What fails is the branch as a whole, because no line yields a Q
    // unconditionally.
    //
    // Deliberately *not* asserted by searching that hole. Doing so exhausts the
    // heap: every forward move adds a resource, which multiplies the forward
    // moves available next, so the branching factor grows without bound. That
    // is the known unbounded-move-space problem, and answering "is this hole
    // provable?" needs the terminating LJT oracle rather than this search — see
    // Phase 6 in /Roadmap.md.
    val premature =
      GameTree.start(distributivity).take("⟶.I").take("∨.I₁").take("∧.I")

    assertEquals(premature.current.position.status, Status.Open, "moves remain available")

    val qHole = premature.current.position.holes
      .find((_, hole) => hole.con == Q)
      .getOrElse(fail("expected an open … : Q"))
      ._2

    // What *is* cheap to check: nothing in scope is a Q, so the hole cannot be
    // closed directly — the player must go through the disjunction.
    val labels = Move.at(qHole).map(_.label).toList
    assert(!labels.contains("Ax"), s"a Q should not be available outright: $labels")
    assert(labels.nonEmpty, "but the position is not stuck either")
  }

  test("backtracking returns to an earlier position without losing the branch") {
    val g = GameTree.start(distributivity).take("⟶.I")
    val afterLeft = g.take("∨.I₁")
    val back = afterLeft.goTo(g.currentId)

    assertEquals(back.currentId, g.currentId)
    assertEquals(back.current.position.holes.length, 1)
    // The abandoned branch is still in the tree — nothing is ever destroyed.
    assert(back.nodes.contains(afterLeft.currentId))
    assertEquals(back.size, 3)
  }

  test("replaying a move reuses its child instead of duplicating it") {
    // The soundness fix. Children are keyed by (hole, move), so exploring a
    // move, backtracking and taking it again is navigation, not a second
    // child — which is what stops an exhaustion count from concluding
    // "everything has been tried" while options remain untried.
    val root = GameTree.start(distributivity)
    val once = root.take("⟶.I")
    val backAtRoot = once.goTo(root.currentId)
    val twice = backAtRoot.take("⟶.I")

    assertEquals(twice.currentId, once.currentId, "same move, same child")
    assertEquals(twice.size, 2, "no duplicate node was created")
    assertEquals(twice.nodes(root.currentId).children.size, 1)
  }

  test("options are keyed distinctly per hole and per move") {
    val g = GameTree.start((A /\ B) ==> (B /\ A)).take("⟶.I").take("∧.I")
    val keys = g.options.map(_._1)
    assertEquals(keys.distinct.length, keys.length, "no key collides")
    // Two open holes now, and each offers its own moves.
    assertEquals(g.current.position.holes.length, 2)
    assert(keys.map(_.hole).distinct.length == 2)
  }

  test("a position part-way through has a partial term but no complete one") {
    val g = GameTree.start(distributivity).take("⟶.I")
    assertEquals(ToLambda.complete(g.current.position), None)
    assert(!g.current.position.isComplete)
  }

  test("a position with holes renders as the Play screen shows it") {
    // What `fold` buys over `term`: an unfinished position is renderable,
    // holes and all. This is the term card mid-game.
    val g = GameTree.start(distributivity).take("⟶.I").take("∧.E₂", forward = true)
    val shown = ToScala(ToLambda.position(g.current.position))
    assertEquals(
      shown,
      "(pqr: (P, Either[Q, R])) => val qr: Either[Q, R] = pqr._2; … : Either[(P, Q), (P, R)]"
    )
  }

  test("every term the engine builds type-checks") {
    // Play a range of goals to a win and audit each finished term with the
    // checker, which knows nothing about the rules that produced it.
    val goals = List(
      A ==> A,
      A ==> (B ==> A),
      (A /\ B) ==> (B /\ A),
      Formula.False ==> A,
      distributivity
    )
    goals.foreach { goal =>
      val position = Search.solve(goal, 12).getOrElse(fail(s"expected a proof of $goal"))
      val term = ToLambda.complete(position).get
      assertEquals(TypeCheck.check(term, goal), Right(()), s"ill-typed term for $goal")
    }
  }
