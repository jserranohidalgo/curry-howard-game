package curryhoward.engine

import munit.FunSuite
import cats.syntax.show.*
import form.{Form, Formula, Notation}
import term.{Lambda, Rendering}
import calculus.*
import calculus.nj.NJ
import calculus.SearchSpace
import calculus.Proof.program

/** The rule set of specification §3.2, exercised. */
class NJSuite extends FunSuite:

  import Formula.given
  val F = Form[Formula]
  import F.{atom, implies, and, or}

  val P = "P".atom
  val Q = "Q".atom
  val R = "R".atom
  val A = "A".atom
  val B = "B".atom
  val C = "C".atom

  private def prove(goal: Formula, maxDepth: Int = 8): Option[Lambda] =
    val space = SearchSpace[Formula, NJ](goal)
    SearchStrategy.iterativeDeepening(maxDepth).apply(space).headOption.map(_.program[Lambda])

  private def scala(t: Lambda): String = Rendering.scala(t)

  test("identity: A => A") {
    val t = prove(A implies A)
    assert(t.isDefined, "A => A should be inhabited")
    assertEquals(scala(t.get), "(a: A) => a")
  }

  test("the K combinator: A => (B => A)") {
    val t = prove(A implies (B implies A))
    assert(t.isDefined)
    assertEquals(scala(t.get), "(a: A) => (b: B) => a")
  }

  test("commutativity: (A, B) => (B, A)") {
    val t = prove((A and B) implies (B and A))
    assert(t.isDefined, "commutativity should be inhabited")
  }

  test("⊤ is inhabited by ()") {
    assertEquals(prove(F.True).map(scala), Some("()"))
  }

  test("⊥ is not inhabited") {
    assertEquals(prove(F.False), None)
  }

  test("ex falso: Nothing => A") {
    val t = prove(F.False implies A)
    assert(t.isDefined, "⊥ => A should be inhabited")
  }

  test("distributivity — the §4.9 playthrough goal") {
    val goal = (P and (Q or R)) implies ((P and Q) or (P and R))
    val t = prove(goal, maxDepth = 10)
    assert(t.isDefined, "distributivity should be inhabited")
  }

  test("excluded middle is NOT inhabited (intuitionistically)") {
    // A ∨ ¬A, with ¬A = A → ⊥
    assertEquals(prove(A or (A implies F.False), maxDepth = 6), None)
  }

  test("both notations render the same goal") {
    val goal = (P and (Q or R)) implies ((P and Q) or (P and R))
    assertEquals(
      Notation.logician[Formula].show(goal),
      "p ∧ (q ∨ r) → p ∧ q ∨ p ∧ r"
    )
    // Note the double bracket on the domain: one argument that is a pair, not
    // two arguments. This is the §4.9 signature verbatim.
    assertEquals(
      Notation.programmer[Formula].show(goal),
      "((P, Either[Q, R])) => Either[(P, Q), (P, R)]"
    )
  }

  test("a product domain is bracketed as one argument, not two") {
    assertEquals(
      Notation.programmer[Formula].show((A and B) implies C),
      "((A, B)) => C"
    )
    assertEquals(
      Notation.programmer[Formula].show(A implies (B implies C)),
      "A => B => C"
    )
    assertEquals(
      Notation.programmer[Formula].show((A implies B) implies C),
      "(A => B) => C"
    )
  }

  test("the distributivity term is the §4.9 playthrough's term") {
    val goal = (P and (Q or R)) implies ((P and Q) or (P and R))
    val src = scala(prove(goal, maxDepth = 10).get)
    // The specification's worked playthrough, down to the forward extraction
    // (`val qr = pqr._2`) that brings the disjunction into scope before the
    // case split, and the names read off the types.
    assertEquals(
      src,
      "(pqr: (P, Either[Q, R])) => val qr: Either[Q, R] = pqr._2; " +
        "qr match { case Left(q: Q) => Left((pqr._1, q)); case Right(r: R) => Right((pqr._1, r)) }"
    )
    // A parameter ascription takes the tuple type as-is; the double bracket is
    // only needed when a pair is the domain of a function *type*.
    assert(!src.startsWith("(pqr: ((P"), "over-bracketed parameter ascription")
  }

  test("negation is re-sugared in logician notation only") {
    val notA = A implies F.False
    assertEquals(Notation.logician[Formula].show(notA), "¬a")
    assertEquals(Notation.programmer[Formula].show(notA), "A => Nothing")
  }

  test("legal moves at the opening position of §4.9") {
    val goal = (P and (Q or R)) implies ((P and Q) or (P and R))
    val moves = SearchSpace.moves[Formula, NJ](Sequent.initial(goal)).toList
    // Empty scope and a function goal: the lambda is the only move available.
    assertEquals(moves.map(NJ.label(_)), List("⟶.I"))
  }
