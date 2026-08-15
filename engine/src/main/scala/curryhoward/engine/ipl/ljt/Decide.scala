package curryhoward.engine
package ipl
package ljt

/** The decision procedure: is this goal provable in intuitionistic
  * propositional logic?
  *
  * A rule proves a sequent when all of its premises are provable, so the whole
  * procedure is one line over [[LJT.coalg]]. What makes that line *total* is
  * the calculus: every LJT rule replaces a formula with its parts, so backward
  * search cannot loop, and no depth bound is needed or wanted. Contrast the
  * game's own search, which needs a move budget precisely because its forward
  * moves accumulate rather than decompose.
  *
  * This is the oracle Phase 6 cross-checks against — the honest answer to "is
  * this a theorem?", as opposed to the game's "did we manage to finish it".
  */
object Decide:

  /** Provable, in the intuitionistic sense. Terminates on every input. */
  def provable(seq: Sequent): Boolean =
    LJT.coalg(seq).exists(rule => LJT.subgoals(rule).forall(provable))

  def provable(goal: Formula): Boolean = provable(Sequent.initial(goal))

  /** The witness, when there is one. */
  def proof(seq: Sequent): Option[Derivation] =
    def attempt(rule: LJT[Sequent]): Option[Derivation] =
      val premises = LJT.subgoals(rule).map(proof)
      Option.when(premises.forall(_.isDefined))(
        Derivation(LJT.label(rule), premises.flatten)
      )

    LJT.coalg(seq).flatMap(rule => attempt(rule).toList).headOption

  def proof(goal: Formula): Option[Derivation] = proof(Sequent.initial(goal))

/** A finished derivation, kept as its shape: rule names and premises.
  *
  * Enough to show *why* something is provable, and no more — LJT has no term
  * assignment here, because it is not played. Herbelin's λ̄ arrives with the LJ
  * game (D22).
  */
final case class Derivation(rule: String, premises: List[Derivation]):
  def render(indent: String = ""): String =
    s"$indent$rule\n" + premises.map(_.render(indent + "  ")).mkString

  def size: Int = 1 + premises.map(_.size).sum
