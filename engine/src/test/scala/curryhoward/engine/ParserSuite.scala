package curryhoward.engine

import munit.ScalaCheckSuite
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Prop.forAll
import ipl.*
import ipl.Formula.*
import ipl.Formula.Syntax.atom

/** Reading a goal, in either notation. */
class ParserSuite extends ScalaCheckSuite:

  val A = "A".atom
  val B = "B".atom
  val C = "C".atom
  val P = "P".atom
  val Q = "Q".atom
  val R = "R".atom

  def parsed(src: String): Formula =
    Parser.parse(src).fold(e => fail(s"did not parse: $src ($e)"), identity)

  // --- Both notations, and mixtures of them ---------------------------------

  test("the same goal in either notation") {
    val expected = Implies(And(P, Or(Q, R)), Or(And(P, Q), And(P, R)))
    assertEquals(parsed("(P, Either[Q, R]) => Either[(P, Q), (P, R)]"), expected)
    assertEquals(parsed("p ∧ (q ∨ r) → (p ∧ q) ∨ (p ∧ r)"), expected)
    assertEquals(parsed("p /\\ (q \\/ r) -> (p /\\ q) \\/ (p /\\ r)"), expected)
  }

  test("notations may be mixed freely") {
    // Nobody would write this, but refusing it would be teaching that the two
    // notations are different things, which is the opposite of the lesson.
    assertEquals(parsed("(P, Q) → p and q"), Implies(And(P, Q), And(P, Q)))
  }

  test("p and P are the same atom") {
    assertEquals(parsed("a => A"), Implies(A, A))
  }

  test("implication is right-associative and loosest") {
    assertEquals(parsed("A => B => C"), Implies(A, Implies(B, C)))
    assertEquals(parsed("A ∧ B => C"), Implies(And(A, B), C))
  }

  test("conjunction binds tighter than disjunction") {
    assertEquals(parsed("A ∧ B ∨ C"), Or(And(A, B), C))
    assertEquals(parsed("A ∨ B ∧ C"), Or(A, And(B, C)))
  }

  test("negation desugars to A → ⊥ however it is written") {
    val notA = Implies(A, False)
    assertEquals(parsed("¬A"), notA)
    assertEquals(parsed("~A"), notA)
    assertEquals(parsed("!A"), notA)
    assertEquals(parsed("not A"), notA)
    assertEquals(parsed("A => Nothing"), notA)
    assertEquals(parsed("¬¬A"), Implies(notA, False))
  }

  test("units are written four ways each") {
    assertEquals(parsed("Unit"), True)
    assertEquals(parsed("⊤"), True)
    assertEquals(parsed("Nothing"), False)
    assertEquals(parsed("⊥"), False)
  }

  // --- Errors carry a code and a position, not a sentence -------------------

  test("empty input") {
    assertEquals(Parser.parse("   "), Left(ParseError.Empty))
  }

  test("an unknown character reports where it is") {
    Parser.parse("A ⊗ B") match
      case Left(ParseError.UnknownChar('⊗', at)) => assertEquals(at, 2)
      case other                                 => fail(s"expected an unknown character, got $other")
  }

  test("unclosed bracket, missing comma, trailing input") {
    assert(Parser.parse("Either[A, B").isLeft, "an unclosed bracket must fail")
    Parser.parse("Either[A B]") match
      case Left(ParseError.ExpectedComma(_)) => ()
      case other                             => fail(s"expected a missing comma, got $other")
    Parser.parse("(A") match
      case Left(ParseError.ExpectedCloseParen(_)) => ()
      case other                                  => fail(s"expected an unclosed paren, got $other")
    Parser.parse("A B") match
      case Left(ParseError.TrailingInput(at)) => assertEquals(at, 2)
      case other                              => fail(s"expected trailing input, got $other")
  }

  test("an incomplete goal fails rather than guessing") {
    assert(Parser.parse("A =>").isLeft)
    assert(Parser.parse("∧ B").isLeft)
  }

  // --- Goals ----------------------------------------------------------------

  test("type parameters are the distinct atoms, in order of appearance") {
    assertEquals(
      Parser.parseGoal("(P, Either[Q, R]) => Either[(P, Q), (P, R)]").map(_.tyParams),
      Right(List("P", "Q", "R"))
    )
    assertEquals(Parser.parseGoal("Unit => ⊥").map(_.tyParams), Right(Nil))
  }

  test("every seeded example parses, and both notations agree") {
    Examples.all.foreach { ex =>
      val fromProgrammer = parsed(ex.programmer)
      val fromLogician = parsed(ex.logician)
      assertEquals(fromProgrammer, fromLogician, s"the two notations of ${ex.id} disagree")
    }
  }

  // --- The round trip -------------------------------------------------------

  val genFormula: Gen[Formula] =
    def go(depth: Int): Gen[Formula] =
      val leaf = Gen.oneOf(Gen.const(True), Gen.const(False), Gen.oneOf(A, B, C, P))
      if depth <= 0 then leaf
      else
        Gen.frequency(
          2 -> leaf,
          3 -> Gen.zip(go(depth - 1), go(depth - 1)).map(Implies(_, _)),
          2 -> Gen.zip(go(depth - 1), go(depth - 1)).map(And(_, _)),
          2 -> Gen.zip(go(depth - 1), go(depth - 1)).map(Or(_, _))
        )
    go(4)

  given Arbitrary[Formula] = Arbitrary(genFormula)

  property("parse ∘ programmer = identity") {
    forAll { (f: Formula) => Parser.parse(Notation.programmer(f)) == Right(f) }
  }

  property("parse ∘ logician = identity") {
    forAll { (f: Formula) => Parser.parse(Notation.logician(f)) == Right(f) }
  }

  property("both notations of a formula parse to the same thing") {
    forAll { (f: Formula) =>
      Parser.parse(Notation.programmer(f)) == Parser.parse(Notation.logician(f))
    }
  }
