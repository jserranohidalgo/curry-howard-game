package curryhoward.engine

import munit.ScalaCheckSuite
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Prop.forAll

import ipl.*
import ipl.nj.*
import ipl.nj.Partial.*
import ipl.ljt.Decide
import Formula.Syntax.atom
import GameTree.NodeId

/** The negative ending, finite case (§4.7, Phase 6, D13).
  *
  * What is being tested here is **soundness**, not reach: the game may leave a
  * goal open for ever, but it must never call a theorem refuted.
  */
class ExhaustionSuite extends ScalaCheckSuite:

  val P = "P".atom
  val Q = "Q".atom
  val R = "R".atom

  val excludedMiddle: Formula = P \/ (P ==> Formula.False)
  val distributivity: Formula = (P /\ (Q \/ R)) ==> ((P /\ Q) \/ (P /\ R))

  /** Play everything, depth-first, the way a determined player would — under
    * two limits, because most of these spaces are infinite: a node budget, and
    * a depth cap that stops the walk diving for ever down a chain of `let`s.
    *
    * Returns the tree explored and whether the walk **finished on its own
    * terms** — that is, whether it stopped because there was nothing left
    * rather than because a limit cut it short. Only a walk that finished says
    * anything about exhaustion; a cut one refutes nothing, which is exactly
    * the invariant of D13.
    */
  private def exploreAll(from: GameTree, budget: Int, maxDepth: Int = 8): (GameTree, Boolean) =
    var cut = false
    def go(tree: GameTree, id: NodeId, depth: Int, left: Int): (GameTree, Int) =
      if left <= 0 then { cut = true; (tree, 0) }
      else if depth >= maxDepth then { cut = true; (tree, left) }
      else
        tree.nodes(id).position.status match
          case Status.Won | Status.Dead => (tree, left)
          case Status.Open =>
            tree.optionsAt(id).foldLeft((tree, left)):
              case ((t, 0), _) => { cut = true; (t, 0) }
              case ((t, b), (key, _)) =>
                val played = t.goTo(id).play(key.hole, key.moveIndex)
                if played.current.position.status == Status.Won then (played, 0)
                else go(played, played.currentId, depth + 1, b - 1)

    val (explored, _) = go(from, from.rootId, 0, budget)
    (explored, !cut)

  private def solved(tree: GameTree): Boolean =
    tree.nodes.values.exists(_.position.status == Status.Won)

  // --- The ending itself ------------------------------------------------------

  test("excluded middle reaches the negative ending by genuine exhaustion") {
    // P ∨ ¬P: two openings, and both die within two moves. Nothing here is
    // decided by a bound — the whole space is walked.
    val (explored, complete) = exploreAll(GameTree.start(excludedMiddle), budget = 100)

    assert(complete, "the search space of excluded middle should be finite")
    assert(!solved(explored), "excluded middle is not a theorem")
    assert(explored.refuted, "every play was tried and none proves it")
    assertEquals(Decide.provable(excludedMiddle), false)
  }

  test("the opening of a refuted goal is exhausted only once *both* openings are") {
    // Explored one disjunct at a time: refuted must stay false until the last
    // pair at the root has been played out.
    val root = GameTree.start(excludedMiddle)
    val left = root.play(Nil, 0) // ∨.I₁ — a hole of type P, with nothing in scope
    assertEquals(left.current.position.status, Status.Dead)
    assert(!left.refuted, "one branch dead is not a refutation")

    val both = exploreAll(left.goTo(left.rootId), budget = 100)._1
    assert(both.refuted)
  }

  test("a theorem is never refuted, however much of it is explored") {
    // The walk cannot reach §4.9's win — it is twelve moves down, past the
    // depth cap — but that is the point: running out of room refutes nothing.
    val (explored, complete) = exploreAll(GameTree.start(distributivity), budget = 400)
    assert(!complete, "distributivity's space is infinite; the walk should be cut")
    assert(!explored.refuted)
  }

  test("the §4.9 winning line is a solution, and a solved tree is never refuted") {
    def take(tree: GameTree, label: String): GameTree =
      tree.options.find((_, move) => NJ.label(move) == label) match
        case Some((key, _)) => tree.play(key.hole, key.moveIndex)
        case None           => fail(s"no $label available")

    // The `let` is chosen by what it binds, not by a rule name — the value is
    // a hole, so every `let` here is labelled the same.
    val wanted = take(GameTree.start(distributivity), "⟶.I")
    val bound = wanted.options.find((_, m) => NJ.binds(m).contains(Q \/ R)) match
      case Some((key, _)) => wanted.play(key.hole, key.moveIndex)
      case None           => fail("no let binding Either[Q, R]")

    val won = List("∧.E₂", "∨.E", "∨.I₁", "∧.I", "∧.E₁", "Ax", "∨.I₂", "∧.I", "∧.E₁", "Ax")
      .foldLeft(bound)(take)

    assertEquals(won.current.position.status, Status.Won)
    assert(solved(won))
    assert(!won.refuted)
  }

  test("an unexplored branch leaves the game open, not refuted") {
    val opened = GameTree.start(excludedMiddle)
    assert(!opened.refuted, "nothing has been tried yet")
  }

  // --- The soundness bug this phase exists to prevent -------------------------

  test("replaying a move from an earlier node cannot manufacture a refutation") {
    // The prototype's failure mode: it counts children against available pairs,
    // so playing one pair twice makes the count reach the threshold with pairs
    // still untried — and announces "This is not a theorem!" about a theorem.
    val start = GameTree.start(P ==> (P \/ Q)) // provable, via ∨.I₁ then Ax
    val opened = start.play(Nil, 0)            // ⟶.I
    val pairs = opened.optionsAt(opened.currentId).length
    assert(pairs > 1, "this position should offer a choice")

    // Take one pair, jump back, take the same pair again — as often as it takes
    // to reach the count the buggy version compares against.
    val replayed = (1 to pairs + 2).foldLeft(opened): (tree, _) =>
      val here = tree.goTo(opened.currentId)
      val (key, _) = tree.optionsAt(opened.currentId).head
      here.play(key.hole, key.moveIndex)

    val childrenOfOpened = replayed.nodes(opened.currentId).children
    assertEquals(childrenOfOpened.size, 1, "one pair, one child, however often it is replayed")
    assert(!replayed.exhausted(opened.currentId), "pairs are still untried")
    assert(!replayed.refuted)
    assertEquals(Decide.provable(P ==> (P \/ Q)), true)
  }

  test("a dead position is exhausted without exploring anything") {
    // P => Q: after ⟶.I the hole admits no rule at all, so there is nothing to
    // walk. Pruning here is what makes exhaustion affordable elsewhere.
    val dead = GameTree.start(P ==> Q).play(Nil, 0)
    assertEquals(dead.current.position.status, Status.Dead)
    assert(dead.exhausted(dead.currentId))
    assert(dead.refuted, "the only opening leads nowhere")
  }

  // --- The corpus -------------------------------------------------------------

  /** Small goals, decided by LJT, played out by the game as far as a budget
    * allows. Each is either refuted, solved, or left open — and every one of
    * those has to agree with the oracle.
    */
  val corpus: List[(String, Formula)] = List(
    "excluded middle" -> excludedMiddle,
    "P => Q" -> (P ==> Q),
    "P => P" -> (P ==> P),
    "P => (Q => P)" -> (P ==> (Q ==> P)),
    "P ∧ Q => Q ∧ P" -> ((P /\ Q) ==> (Q /\ P)),
    "P => P ∨ Q" -> (P ==> (P \/ Q)),
    "P ∨ Q => Q ∨ P" -> ((P \/ Q) ==> (Q \/ P)),
    "¬¬P => P" -> (((P ==> Formula.False) ==> Formula.False) ==> P),
    "P => ¬¬P" -> (P ==> ((P ==> Formula.False) ==> Formula.False)),
    "⊥ => P" -> (Formula.False ==> P),
    "P => ⊤" -> (P ==> Formula.True),
    "(P => Q) ∧ P => Q" -> (((P ==> Q) /\ P) ==> Q),
    "Peirce" -> ((((P ==> Q) ==> P) ==> P))
  )

  test("the corpus is decided or left open, never wrongly") {
    corpus.foreach { (name, goal) =>
      val (explored, complete) = exploreAll(GameTree.start(goal), budget = 200, maxDepth = 6)
      val provable = Decide.provable(goal)

      if explored.refuted then
        assert(!provable, s"$name: refuted by the game, but LJT proves it")
      if solved(explored) then
        assert(provable, s"$name: solved by the game, but LJT says it is not a theorem")
      if complete && !solved(explored) then
        assert(explored.refuted, s"$name: the space was walked out with no solution, so it should be refuted")
      // Anything else is an open game, which is the honest state of an
      // undecided search (D13) — ¬¬P => P is expected to land here.
    }
  }

  test("double-negation elimination stays open rather than being refuted") {
    // The deferred half of D13: the space is infinite, so the walk runs out of
    // budget. It must not conclude anything from that.
    val dne = ((P ==> Formula.False) ==> Formula.False) ==> P
    val (explored, complete) = exploreAll(GameTree.start(dne), budget = 200, maxDepth = 6)
    assert(!complete, "expected an infinite space to outlast the budget")
    assert(!explored.refuted, "an unfinished walk refutes nothing")
    assert(!solved(explored))
    assertEquals(Decide.provable(dne), false) // true classically, not here
  }

  // --- The property -----------------------------------------------------------

  val genSmall: Gen[Formula] =
    def go(depth: Int): Gen[Formula] =
      val leaf = Gen.oneOf[Formula](P, Q, Formula.True, Formula.False)
      if depth <= 0 then leaf
      else
        Gen.frequency(
          3 -> leaf,
          3 -> Gen.zip(go(depth - 1), go(depth - 1)).map(Formula.Implies(_, _)),
          2 -> Gen.zip(go(depth - 1), go(depth - 1)).map(Formula.And(_, _)),
          2 -> Gen.zip(go(depth - 1), go(depth - 1)).map(Formula.Or(_, _))
        )
    go(3)

  given Arbitrary[Formula] = Arbitrary(genSmall)

  property("the game never refutes a theorem") {
    forAll { (goal: Formula) =>
      val (explored, _) = exploreAll(GameTree.start(goal), budget = 60, maxDepth = 5)
      !explored.refuted || !Decide.provable(goal)
    }
  }

  property("what the game finishes, LJT proves") {
    forAll { (goal: Formula) =>
      val (explored, _) = exploreAll(GameTree.start(goal), budget = 60, maxDepth = 5)
      !solved(explored) || Decide.provable(goal)
    }
  }
