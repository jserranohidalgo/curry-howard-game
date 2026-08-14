package curryhoward.engine

/** Placeholder for the game engine.
  *
  * Phase 2 replaces this with the real thing: the type and term models, holes
  * and per-hole scope, legal-move generation, move application and the game
  * tree — re-implemented from `doc/mockup/design/logic.jsx`, which is the
  * specification for all of it.
  *
  * For now it exists so that Phase 1 can prove the wiring end to end: this
  * object is compiled for both the JVM (where the tests run) and Scala.js
  * (where the web app links against it).
  */
object Engine:
  val name: String = "curry-howard-engine"
  val version: String = "0.1.0-SNAPSHOT"

  /** What the engine can do so far. Phase 2 makes this list interesting. */
  def capabilities: List[String] = Nil
