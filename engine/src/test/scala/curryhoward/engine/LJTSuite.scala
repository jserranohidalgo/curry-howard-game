package curryhoward.engine

import munit.{FunSuite, ScalaCheckSuite}
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Prop.forAll
import ipl.*
import ipl.Formula.Syntax.atom
import ipl.ljt.Decide
import ipl.nj.{SearchSpace, SearchStrategy, Proof as NJProof}

/** The oracle: Dyckhoff's LJT deciding intuitionistic provability. */
class LJTSuite extends ScalaCheckSuite:

  val A = "A".atom
  val B = "B".atom
  val C = "C".atom
  val P = "P".atom
  val Q = "Q".atom
  val R = "R".atom

  def not(f: Formula): Formula = f ==> Formula.False

  // --- Theorems --------------------------------------------------------------

  test("identity and the K combinator") {
    assert(Decide.provable(A ==> A))
    assert(Decide.provable(A ==> (B ==> A)))
  }

  test("distributivity — the §4.9 goal") {
    assert(Decide.provable((P /\ (Q \/ R)) ==> ((P /\ Q) \/ (P /\ R))))
  }

  test("commutativity, transitivity, currying") {
    assert(Decide.provable((A /\ B) ==> (B /\ A)))
    assert(Decide.provable((A ==> B) ==> ((B ==> C) ==> (A ==> C))))
    assert(Decide.provable(((A /\ B) ==> C) ==> (A ==> (B ==> C))))
  }

  test("⊤, ⊥ and negation") {
    assert(Decide.provable(Formula.True))
    assert(!Decide.provable(Formula.False))
    assert(Decide.provable(Formula.False ==> A))
    assert(Decide.provable(A ==> not(not(A))), "double negation introduction is intuitionistic")
    assert(Decide.provable(not(not(not(A))) ==> not(A)), "triple negation collapses")
  }

  // --- The classical non-theorems -------------------------------------------
  // These are the point of the oracle. Each is classically valid and
  // intuitionistically not, and each is a goal the game's own search cannot
  // settle — it either exhausts a finite tree (excluded middle) or runs
  // forever (the rest).

  test("excluded middle is not intuitionistically provable") {
    assert(!Decide.provable(A \/ not(A)))
  }

  test("double-negation elimination is not provable — §4.10's infinite tree") {
    assert(!Decide.provable(not(not(A)) ==> A))
  }

  test("Peirce's law is not provable") {
    assert(!Decide.provable(((A ==> B) ==> A) ==> A))
  }

  test("de Morgan: the classical direction fails, the others hold") {
    assert(!Decide.provable(not(A /\ B) ==> (not(A) \/ not(B))), "the classical one")
    assert(Decide.provable(not(A \/ B) ==> (not(A) /\ not(B))))
    assert(Decide.provable((not(A) \/ not(B)) ==> not(A /\ B)))
  }

  test("linearity and material implication are classical only") {
    assert(!Decide.provable((A ==> B) \/ (B ==> A)))
    assert(!Decide.provable((A ==> B) ==> (not(A) \/ B)))
  }

  // --- Termination -----------------------------------------------------------
  // The property that makes LJT worth having. The game's own search has no such
  // guarantee: asking it about `… : Q` from `pqr: P ∧ (Q ∨ R)` exhausted the
  // heap after 29 minutes.

  val genFormula: Gen[Formula] =
    def go(depth: Int): Gen[Formula] =
      if depth <= 0 then Gen.oneOf(Gen.const(Formula.True), Gen.const(Formula.False), Gen.oneOf(A, B, C))
      else
        Gen.frequency(
          2 -> Gen.oneOf(Gen.const(Formula.True), Gen.const(Formula.False), Gen.oneOf(A, B, C)),
          3 -> Gen.zip(go(depth - 1), go(depth - 1)).map(_ ==> _),
          2 -> Gen.zip(go(depth - 1), go(depth - 1)).map(_ /\ _),
          2 -> Gen.zip(go(depth - 1), go(depth - 1)).map(_ \/ _)
        )
    go(3)

  given Arbitrary[Formula] = Arbitrary(genFormula)

  property("LJT decides every formula, without a depth bound") {
    forAll { (f: Formula) =>
      // No assertion on the verdict: the claim is that one arrives at all.
      val verdict = Decide.provable(f)
      verdict == verdict
    }
  }

  property("a proof is produced exactly when the goal is provable") {
    forAll { (f: Formula) =>
      Decide.proof(f).isDefined == Decide.provable(f)
    }
  }

  // --- Cross-check against the game -----------------------------------------

  property("anything the game finishes, LJT calls provable") {
    forAll { (f: Formula) =>
      val played = SearchStrategy.iterativeDeepening(6).apply(SearchSpace(f)).headOption
      // One direction only: the game's bounded search may miss a theorem, but
      // it must never finish a non-theorem. That is soundness, and it is what
      // Phase 6 leans on.
      played.isEmpty || Decide.provable(f)
    }
  }
