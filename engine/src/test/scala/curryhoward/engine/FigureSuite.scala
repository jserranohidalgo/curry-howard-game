package curryhoward.engine

import munit.FunSuite
import ipl.*
import ipl.nj.*
import ipl.nj.Figure.*
import ipl.nj.Partial.*
import Formula.Syntax.atom

/** The logician's reading of a position (Phase 7, D25).
  *
  * The target is fixed and written down: §4.9 of the specification draws the
  * finished derivation, and D25 draws what the intermediate states must show.
  * These tests are those two figures.
  */
class FigureSuite extends FunSuite:

  val P = "P".atom
  val Q = "Q".atom
  val R = "R".atom
  val distributivity: Formula = (P /\ (Q \/ R)) ==> ((P /\ Q) \/ (P /\ R))

  extension (tree: GameTree)
    def take(label: String): GameTree =
      tree.options.find((_, move) => NJ.label(move) == label) match
        case Some((key, _)) => tree.play(key.hole, key.moveIndex)
        case None           => fail(s"no $label available")
    def takeLet(tpe: Formula): GameTree =
      tree.options.find((_, m) => NJ.binds(m).contains(tpe)) match
        case Some((key, _)) => tree.play(key.hole, key.moveIndex)
        case None           => fail(s"no let binding ${Notation.programmer(tpe)}")

    /** Play a move at a hole with a given goal — needed when several holes are
      * open and the first one is not the one meant.
      */
    def takeAt(goal: Formula, label: String): GameTree =
      tree.options.find { (key, move) =>
        NJ.label(move) == label && tree.current.position.holeAt(key.hole).exists(_.con == goal)
      } match
        case Some((key, _)) => tree.play(key.hole, key.moveIndex)
        case None           => fail(s"no $label at a ${Notation.programmer(goal)} hole")

  private def forest(tree: GameTree): Forest = ToFigure(tree.current.position)

  private def drawn(f: Figure): String = Figure.ascii(f).mkString("\n")

  // --- The opening ------------------------------------------------------------

  test("a fresh game is one open leaf: the goal, underived") {
    val f = forest(GameTree.start(distributivity))
    assertEquals(f.main, Figure.Todo(0, distributivity))
    assert(f.isBare, "nothing has been derived yet")
  }

  test("⟶.I draws one bar and puts a hypothesis in force") {
    val f = forest(GameTree.start(distributivity).take("⟶.I"))
    f.main match
      case Figure.Infer("→I", List(Figure.Todo(0, goal, assumed)), concl, List(1)) =>
        assertEquals(goal, (P /\ Q) \/ (P /\ R))
        assertEquals(concl, distributivity)
        // §3.1 writes →I's premise as `[A] ⋮ B`, and while the branch is empty
        // this is the only place the label's meaning is visible.
        assertEquals(assumed, List(1 -> (P /\ (Q \/ R))), "the antecedent stands over the branch")
      case other => fail(s"expected one →I bar over the hole, got $other")
    assert(f.isBare, "the antecedent is a hypothesis, not a derived fact")
  }

  // --- The move D25 exists for -----------------------------------------------

  test("a forward move leaves the derivation alone and derives a fact beside it") {
    // §4.9's move 2, the composite `∧.E₂`: one stroke, and the logician view
    // must show progress. It shows it here — a new bar in a new fragment.
    val before = forest(GameTree.start(distributivity).take("⟶.I"))
    val after = forest(GameTree.start(distributivity).take("⟶.I").takeLet(Q \/ R).take("∧.E₂"))

    assertEquals(after.derived.length, 1, "the move derived exactly one fact")
    val fact = after.derived.head
    assertEquals(fact.formula, Q \/ R)
    assertEquals(fact.uses, 0, "derived, not yet used")
    assertEquals(
      drawn(fact.figure),
      """[p ∧ (q ∨ r)]¹
        |────────────── ∧E₂
        |    q ∨ r""".stripMargin
    )

    // The main derivation is untouched — which is exactly why the fact has to
    // be shown: without it the move would be invisible to the logician.
    assertEquals(drawn(after.main), drawn(before.main))
  }

  test("using the fact grafts it into the derivation, where §4.9 has it") {
    val after = forest(
      GameTree.start(distributivity).take("⟶.I").takeLet(Q \/ R).take("∧.E₂").take("∨.E")
    )
    assertEquals(after.derived.head.uses, 1, "the fact has been used once")

    after.main match
      case Figure.Infer("→I", List(Figure.Infer("∨E", major :: cases, _, List(2, 3))), _, List(1)) =>
        assertEquals(
          drawn(major),
          """[p ∧ (q ∨ r)]¹
            |────────────── ∧E₂
            |    q ∨ r""".stripMargin,
          "the derived fact is the major premise of ∨E"
        )
        assertEquals(cases.length, 2)
        assert(cases.forall(_.isInstanceOf[Figure.Todo]), "one open leaf per case")
      case other => fail(s"expected →I over ∨E, got $other")
  }

  // --- The finished proof -----------------------------------------------------

  private val won: GameTree =
    GameTree
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
      .take("∧.I")
      .take("∧.E₁")
      .take("Ax")

  test("the finished proof is the normal derivation of §4.9, with no cut in it") {
    val f = forest(won)
    assertEquals(f.main.holes, Nil, "a finished proof has no open leaves")

    // Every bar in the figure, with what it derives — the specification's
    // figure read off the drawing.
    def bars(fig: Figure): List[(String, String)] = fig match
      case Figure.Infer(rule, premises, concl, discharges) =>
        (rule + discharges.mkString(","), Notation.logician(concl)) :: premises.flatMap(bars)
      case _ => Nil

    assertEquals(
      bars(f.main),
      List(
        "→I1" -> "p ∧ (q ∨ r) → p ∧ q ∨ p ∧ r",
        "∨E2,3" -> "p ∧ q ∨ p ∧ r",
        "∧E₂" -> "q ∨ r",
        "∨I₁" -> "p ∧ q ∨ p ∧ r",
        "∧I" -> "p ∧ q",
        "∧E₁" -> "p",
        "∨I₂" -> "p ∧ q ∨ p ∧ r",
        "∧I" -> "p ∧ r",
        "∧E₁" -> "p"
      )
    )
  }

  test("a fact used twice is drawn twice — sharing belongs to the program") {
    val f = forest(won)
    assertEquals(f.derived.length, 1)
    assertEquals(f.derived.head.uses, 1, "the disjunction is scrutinised once")

    // …and the hypothesis behind it appears three times, all with label 1:
    // once as the major premise's premise, and once inside each ∧E₁.
    def hyps(fig: Figure): List[Int] = fig match
      case Figure.Infer(_, premises, _, _) => premises.flatMap(hyps)
      case Figure.Hyp(label, _)            => List(label)
      case Figure.Todo(_, _, _)            => Nil

    assertEquals(hyps(f.main).count(_ == 1), 3, "one hypothesis, three occurrences")
  }

  test("the finished figure, drawn") {
    // The whole point of Phase 7 in one assertion: this is what a player sees.
    val drawing = drawn(forest(won).main)
    assert(drawing.contains("∨E²³"), drawing)
    assert(drawing.contains("→I¹"), drawing)
    assert(drawing.linesIterator.toList.last.trim == "p ∧ (q ∨ r) → p ∧ q ∨ p ∧ r", drawing)
    // Printed once so a human can look at it when this file is touched.
    println(drawing)
  }

  // --- Holes are shared, not copied -------------------------------------------

  test("a fact used twice draws its holes twice, and they are the same holes") {
    // f: A ⟶ B used forward twice binds one value with one argument hole; both
    // drawings of the fragment show that same hole index.
    val A = "A".atom
    val B = "B".atom
    val goal = (A ==> B) ==> (A ==> (B /\ B))
    val played = GameTree.start(goal).take("⟶.I").take("⟶.I").takeLet(B).take("⟶.E").take("∧.I")
    val f = forest(played)

    val fact = f.derived.head
    assertEquals(fact.formula, B)
    assertEquals(fact.figure.holes.length, 1, "the fragment has one hole: the argument")

    // Close both components with the derived fact — at the `B` holes, since
    // the fragment's own argument hole is open too and comes first.
    val twice = played.takeAt(B, "Ax").takeAt(B, "Ax")
    val g = forest(twice)
    assertEquals(g.derived.head.uses, 2, "used once per component")
    val drawnTwice = g.main.holes
    assertEquals(drawnTwice.length, 2, "the argument hole is drawn twice")
    assertEquals(drawnTwice.distinct.length, 1, "and both drawings are the same hole")
  }
