package curryhoward.engine

import munit.ScalaCheckSuite
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Prop.forAll

import ipl.*
import ipl.nj.*
import ipl.nj.Partial.*
import Formula.Syntax.atom

/** Saving a game and reading it back (D4/D5, Phase 5).
  *
  * A tree cannot be compared with `==`: positions are `Mu`, whose payload is a
  * thunk, so equality there is reference equality. The comparison that matters
  * is the one a player would notice — the same shape of tree, the same move on
  * every edge, the same node current, and the same program on screen.
  */
class SaveSuite extends ScalaCheckSuite:

  val P = "P".atom
  val Q = "Q".atom
  val R = "R".atom

  val distributivity: Formula = (P /\ (Q \/ R)) ==> ((P /\ Q) \/ (P /\ R))

  /** What the player can see of a tree. */
  private def shape(tree: GameTree) =
    (
      tree.goal,
      tree.currentId.value,
      tree.nodes.toList
        .map((id, node) => (id.value, node.parentId.map(_.value), node.via, node.depth))
        .sortBy(_._1),
      ToScala(ToLambda.position(tree.current.position))
    )

  private def roundTrip(tree: GameTree)(using munit.Location) =
    Save.decode(Save.encode(tree)) match
      case Left(err)      => fail(s"did not decode: $err")
      case Right(restored) => assertEquals(shape(restored), shape(tree))

  extension (tree: GameTree)
    def take(label: String): GameTree =
      tree.options.find((_, move) => NJ.label(move) == label) match
        case Some((key, _)) => tree.play(key.hole, key.moveIndex)
        case None           => fail(s"no $label available")
    def takeLet(tpe: Formula): GameTree =
      tree.options.find((_, m) => NJ.binds(m).contains(tpe)) match
        case Some((key, _)) => tree.play(key.hole, key.moveIndex)
        case None           => fail(s"no let binding ${Notation.programmer(tpe)}")

  test("a fresh game round-trips") {
    roundTrip(GameTree.start(distributivity))
  }

  test("a game in progress round-trips, and the save is legible") {
    val played = GameTree.start(distributivity).take("⟶.I").takeLet(Q \/ R).take("∧.E₂")
    roundTrip(played)

    val text = Save.encode(played)
    assertEquals(text.linesIterator.next(), Save.Version)
    assertEquals(text.linesIterator.drop(1).next(), Notation.programmer(distributivity))
    assertEquals(text.linesIterator.drop(2).next(), played.currentId.value.toString)
    assertEquals(text.linesIterator.drop(3).length, 3) // one line per move played
  }

  test("a finished game round-trips") {
    val won = GameTree
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

    assertEquals(won.current.position.status, Status.Won)
    roundTrip(won)
  }

  test("a branched tree keeps every branch, and comes back on the node the player was on") {
    // Explore one line, back up, explore another — the whole point of D4/D5 is
    // that the abandoned branch survives the save.
    val opened = GameTree.start(distributivity).take("⟶.I")
    val firstBranch = opened.takeLet(P)
    val backAgain = firstBranch.goTo(opened.currentId)
    val secondBranch = backAgain.takeLet(Q \/ R).take("∧.E₂")
    // …and end up somewhere that is neither branch's tip.
    val parked = secondBranch.goTo(opened.currentId)

    assert(parked.size > 3, "expected both branches in the tree")
    roundTrip(secondBranch)
    roundTrip(parked)
  }

  test("a save written by another rule set is refused, not misread") {
    val text = Save.encode(GameTree.start(distributivity).take("⟶.I"))
    assertEquals(Save.decode(text.replace(Save.Version, "chg0")), Left(Save.SaveError.BadVersion("chg0")))
  }

  test("a move that no longer exists fails the restore") {
    val text = Save.encode(GameTree.start(distributivity).take("⟶.I"))
    val bent = text.linesIterator.map(l => if l.startsWith("1 ") then "1 0 - 99" else l).mkString("\n")
    assertEquals(Save.decode(bent), Left(Save.SaveError.Stale(1)))
  }

  test("junk is refused") {
    assert(Save.decode("").isLeft)
    assert(Save.decode("chg1").isLeft)
    assert(Save.decode(s"${Save.Version}\n(((\n0").isLeft)
  }

  // --- The property ---------------------------------------------------------

  /** A random play: at each step, take one of the moves actually on offer, and
    * now and then jump back to an earlier node, which is what makes the tree a
    * tree rather than a line.
    */
  val genPlay: Gen[GameTree] =
    def go(tree: GameTree, steps: Int): Gen[GameTree] =
      if steps <= 0 then Gen.const(tree)
      else
        val options = tree.options
        if options.isEmpty then Gen.const(tree)
        else
          for
            (key, _) <- Gen.oneOf(options)
            played = tree.play(key.hole, key.moveIndex)
            jump <- Gen.frequency(3 -> Gen.const(false), 1 -> Gen.const(true))
            here <- if jump then Gen.oneOf(played.nodes.keys.toList).map(played.goTo) else Gen.const(played)
            rest <- go(here, steps - 1)
          yield rest

    Gen.choose(0, 8).flatMap(go(GameTree.start(distributivity), _))

  given Arbitrary[GameTree] = Arbitrary(genPlay)

  property("decode ∘ encode = identity, for any play") {
    forAll { (tree: GameTree) =>
      Save.decode(Save.encode(tree)).map(shape) == Right(shape(tree))
    }
  }
