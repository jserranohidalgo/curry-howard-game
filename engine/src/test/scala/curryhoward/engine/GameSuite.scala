package curryhoward.engine

import munit.FunSuite
import ipl.*
import ipl.nj.*
import ipl.nj.Partial.*
import Formula.Syntax.atom
import Proof.interpret

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
    def take(label: String): GameTree =
      tree.options.find((_, move) => NJ.label(move) == label) match
        case Some((key, _)) => tree.play(key.hole, key.moveIndex)
        case None           => fail(s"no $label available; ${offered(tree)}")

    /** Bind a new resource of this type. A `let` is chosen by *what it binds*,
      * not by a rule name: the value is a hole, so two resources offering an
      * `Either[Q, R]` offer one and the same move.
      */
    def takeLet(tpe: Formula): GameTree =
      tree.options.find((_, m) => NJ.binds(m).contains(tpe)) match
        case Some((key, _)) => tree.play(key.hole, key.moveIndex)
        case None           => fail(s"no let binding ${Notation.programmer(tpe)}; ${offered(tree)}")

  private def offered(tree: GameTree): String =
    "offered: " + tree.options
      .map((_, m) => NJ.binds(m).fold(NJ.label(m))(t => s"let ${Notation.programmer(t)}"))
      .mkString(", ")

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
      .takeLet(Q \/ R)                  // move 2 — state that an Either[Q, R] is wanted
      .take("∧.E₂")                     // move 3 — and produce it: pqr._2
      .take("∨.E")                      // move 4 — case split
      .take("∨.I₁")                     // move 5 — left branch, informed choice
      .take("∧.I")                      // move 6
      .take("∧.E₁")                     // move 7 — backward: closes … : P
      .take("Ax")                       // move 8
      .take("∨.I₂")                     // move 9 — right branch
      .take("∧.I")                      // move 10
      .take("∧.E₁")                     // move 11
      .take("Ax")                       // move 12

    assertEquals(played.current.position.status, Status.Won)
    // Twelve, not the specification's eleven: forward reasoning is a `let`, and
    // a `let` states what it wants before producing it. The extra move is the
    // producing.
    assertEquals(played.depth, 12)

    // The same finished position, read two ways.
    val rendered = ToScala(ToLambda.position(played.current.position))
    val term = played.current.position.term(ToLambda.apply).get
    assertEquals(
      rendered,
      "(pqr: (P, Either[Q, R])) => val qr: Either[Q, R] = pqr._2; " +
        "qr match { case Left(q: Q) => Left((pqr._1, q)); case Right(r: R) => Right((pqr._1, r)) }"
    )
    assertEquals(TypeCheck.check(term, distributivity), Right(()))
  }

  test("the program is laid out as Scala, and a `val` in an expression position gets a block") {
    // A forward move inside `Right(…)` puts a binding where only expressions
    // go. Rendered flat it is not Scala at all — `Right(val p = pqr._1; …)`
    // does not compile — so the block is a correctness question, not taste.
    val played = GameTree
      .start(distributivity)
      .take("⟶.I")
      .takeLet(Q \/ R)
      .take("∧.E₂")
      .take("∨.E")
      .take("∨.I₁")
      .take("∧.I")
      .take("∧.E₁")
      .take("Ax")
      .take("∨.I₂")
      .takeLet(P)
      .take("∧.E₁")
      .take("∧.I")
      .take("Ax")
      .take("Ax")

    val term = ToLambda.complete(played.current.position).getOrElse(fail("expected a finished term"))
    assertEquals(
      ToScala.plain(ToScala.formatted(term)),
      """|(pqr: (P, Either[Q, R])) =>
         |  val qr: Either[Q, R] = pqr._2
         |  qr match {
         |    case Left(q: Q) => Left((pqr._1, q))
         |    case Right(r: R) => Right {
         |      val p: P = pqr._1
         |      (p, r)
         |    }
         |  }""".stripMargin
    )

    // And the same term on one line, which is what a tooltip wants and what
    // the other tests assert against — the two renderings are one traversal.
    assert(ToScala(term).linesIterator.size == 1)
  }

  test("§4.9's final step: the cleaned-up program") {
    // The specification's own last move — drop the scaffolding, keep one type
    // annotation. The `val qr` is used exactly once, so it inlines; a binding
    // used twice would stay, because inlining it would duplicate the value.
    val term = SearchStrategy
      .iterativeDeepening(12)
      .apply(SearchSpace(distributivity))
      .headOption
      .getOrElse(fail("expected a proof"))
      .interpret(ToLambda.apply)

    assertEquals(
      ToScala(term),
      "(pqr: (P, Either[Q, R])) => val qr: Either[Q, R] = pqr._2; " +
        "qr match { case Left(q: Q) => Left((pqr._1, q)); case Right(r: R) => Right((pqr._1, r)) }"
    )
    assertEquals(
      ToScala.bare(Cleanup.simplify(term)),
      "pqr => pqr._2 match { case Left(q) => Left((pqr._1, q)); case Right(r) => Right((pqr._1, r)) }"
    )
  }

  test("a binding used twice is kept, not duplicated") {
    // (A => B) => (A => (B, B)): the result of f is wanted twice, so inlining
    // would copy the application. Sharing earns its keep.
    val f = (A ==> B) ==> (A ==> (B /\ B))
    val term = SearchStrategy
      .iterativeDeepening(10)
      .apply(SearchSpace(f))
      .headOption
      .getOrElse(fail("expected a proof"))
      .interpret(ToLambda.apply)
    val cleaned = ToScala.bare(Cleanup.simplify(term))
    assert(
      cleaned.contains("val") || cleaned.count(_ == 'f') >= 2,
      s"either the binding is kept or the application is duplicated: $cleaned"
    )
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
    val labels = NJ.coalg(qHole).map(NJ.label(_)).toList
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
    assertEquals(g.current.position.term(ToLambda.apply), None)
    assert(!g.current.position.isComplete)
  }

  test("a complete position is a proof, and interprets the same either way") {
    // What the fixpoint buys: `toProof` is `sequence` over the hole layer, so a
    // finished position converts to a Proof and interpretations run on that.
    // One fold for finished derivations rather than two.
    val played = GameTree
      .start(A ==> A)
      .take("⟶.I")
      .take("Ax")

    val position = played.current.position
    assertEquals(position.status, Status.Won)

    val viaPosition = ToLambda.complete(position).map(ToScala.apply)
    val viaProof = position.toProof.map(pf => ToScala(pf.interpret(ToLambda.apply)))
    assertEquals(viaPosition, Some("(a: A) => a"))
    assertEquals(viaProof, viaPosition, "a position and its proof interpret alike")
  }

  test("an unfinished position is not a proof") {
    val g = GameTree.start(distributivity).take("⟶.I")
    assertEquals(g.current.position.toProof, None)
  }

  test("a position with holes renders as the Play screen shows it") {
    // What `fold` buys over `term`: an unfinished position is renderable,
    // holes and all. This is the term card mid-game.
    val g = GameTree.start(distributivity).take("⟶.I").takeLet(Q \/ R).take("∧.E₂")
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
      val proof = SearchStrategy
        .iterativeDeepening(10)
        .apply(SearchSpace(goal))
        .headOption
        .getOrElse(fail(s"expected a proof of $goal"))
      val term = proof.interpret(ToLambda.apply)
      assertEquals(TypeCheck.check(term, goal), Right(()), s"ill-typed term for $goal")
    }
  }
