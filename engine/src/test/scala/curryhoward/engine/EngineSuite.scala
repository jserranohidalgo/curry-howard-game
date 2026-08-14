package curryhoward.engine

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

/** Phase 1 smoke test: proves munit and ScalaCheck are both wired up and
  * running on the JVM side of the cross-build. Phase 2 replaces these with the
  * real properties — every term the engine builds type-checks, the §4.9
  * playthrough reproduces move for move, game trees survive a serialization
  * round-trip.
  */
class EngineSuite extends ScalaCheckSuite:

  test("the engine identifies itself") {
    assertEquals(Engine.name, "curry-howard-engine")
  }

  test("no capabilities yet — Phase 2 fills this in") {
    assertEquals(Engine.capabilities, Nil)
  }

  property("ScalaCheck is running: reverse is an involution") {
    forAll { (xs: List[Int]) => xs.reverse.reverse == xs }
  }
