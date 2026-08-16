package curryhoward.repl

import curryhoward.engine.ipl.*
import curryhoward.engine.ipl.nj.*
import curryhoward.engine.ipl.nj.Partial.*
import curryhoward.engine.ipl.ljt.Decide

/** Contextual help, in three escalating steps.
  *
  * The users are students, so being stuck is the expected state rather than a
  * failure, and the app should answer the question a stuck student actually
  * asks — *what am I supposed to do here?* — without answering the one they are
  * meant to work out for themselves until they ask for that too.
  *
  *   - `?`   what am I looking at, and what do I do now
  *   - `??`  what do these particular moves mean, in both vocabularies
  *   - `???` a hint: which moves keep the goal winnable
  *
  * The third is only possible because of the LJT oracle. A hint needs no proof
  * translation — it asks, of each legal move, whether every hole it opens is
  * provable. All provable means the move keeps the game winnable; that is
  * sound, and it terminates.
  */
object Help:

  /** `?` at the goal prompt: what can be typed. */
  def goalPrompt(level: Int): String = level match
    case 1 =>
      s"""${View.panel("what now?")}
         |  Type a *signature* to inhabit, or the *proposition* it corresponds to —
         |  they are the same thing written two ways, and either will do.
         |
         |    (A, B) => (B, A)              a ∧ b → b ∧ a
         |    A => (B => A)                 a → (b → a)
         |    Either[A, A => Nothing]       a ∨ ¬a
         |
         |  Then you fill in the program step by step, and the finished program
         |  is the proof.  `??` for the full syntax.
         |""".stripMargin

    case 2 =>
      s"""${View.panel("syntax")}
         |  implication   =>   ->   →          right-associative, binds loosest
         |  conjunction   (A, B)    a ∧ b      also  &  /\\  and
         |  disjunction   Either[A, B]  a ∨ b  also  |  \\/  or
         |  negation      ¬a   ~a   !a   not a    all mean  a → ⊥
         |  units         Unit  ⊤              Nothing  ⊥
         |
         |  Notations may be mixed: `(P, Q) → p and q` is accepted.
         |  Atoms are case-insensitive — `p` and `P` are the same atom.
         |""".stripMargin

    case _ =>
      s"""${View.panel("suggestion")}
         |  Start with something small and true:
         |
         |    (A, B) => (B, A)         swap a pair
         |    A => (B => A)            the K combinator
         |
         |  Then try one that is *not* provable, to meet the other ending:
         |
         |    Either[A, A => Nothing]  excluded middle
         |""".stripMargin

  /** `?` `??` `???` during play. */
  def position(goal: Goal, position: Partial, view: View, level: Int): String =
    level match
      case 1 => orientation(position, view)
      case 2 => rules(position)
      case _ => hint(goal, position)

  // --- ? --------------------------------------------------------------------

  private def orientation(position: Partial, view: View): String =
    val holes = position.holes
    val here = holes.zipWithIndex
      .map { case ((_, hole), i) =>
        val scope =
          if hole.ant.isEmpty then "nothing in scope"
          else s"${hole.ant.length} resource${if hole.ant.length == 1 then "" else "s"} in scope"
        s"    [${View.holeLabel(i)}]  needs a ${view.show(hole.con)}  ($scope)"
      }
      .mkString("\n")

    val situation =
      if position.status == Status.Dead then
        "  One of your holes has no legal move at all. That branch is finished —\n" +
          "  `back` to an earlier position and try something else. Nothing is lost."
      else if holes.length == 1 then
        "  One hole left to fill."
      else
        s"  ${holes.length} holes left. You may work on any of them; each move says which."

    s"""${View.panel("what now?")}
       |  You are writing a program that fits the goal. Because of the
       |  Curry–Howard correspondence, the finished program *is* a proof of it.
       |
       |  program     what you have so far. `… : T` is a hole — something of
       |              type T still has to go there.
       |  resources   what you may use at each hole. A hole is filled either by
       |              building its type, or by taking something apart.
       |  moves       every legal move, numbered. Type a number to play it.
       |
       |$situation
       |
       |$here
       |
       |  `??` explains the moves on offer.  `???` gives a hint.
       |""".stripMargin

  // --- ?? -------------------------------------------------------------------

  private def rules(position: Partial): String =
    val offers = Moves.offers(position)

    // Grouped by *explanation*, not by label: ∨.I₁ and ∨.I₂ are one lesson,
    // and so are the two projections.
    val explained = offers
      .groupBy(o => meaning(NJ.label(o.move)))
      .toList
      .sortBy(_._2.map(_.number).min)
      .map { case (text, group) =>
        f"  ${group.map(_.number).mkString(", ")}%-8s $text"
      }
      .mkString("\n\n")

    s"""${View.panel("what these moves mean")}
       |$explained
       |
       |  In the table, `·` marks a rule that exists but does not apply here —
       |  it is shown rather than hidden because the table is the whole syllabus.
       |  `—` marks a rule that does not exist at all: ⊥ has no introduction
       |  (nothing proves a contradiction), ⊤ has no elimination (knowing ⊤
       |  tells you nothing), and a hypothesis cannot be introduced.
       |""".stripMargin

  private def meaning(label: String): String = label match
    case "⟶.I" =>
      "⟶.I — assume the argument, then prove the result.\n" +
        "           program: write a lambda.  logic: to prove A → B, assume A and prove B."
    case "⟶.E" =>
      "⟶.E — use a function you have.\n" +
        "           program: apply it.  logic: from A → B and A, conclude B (modus ponens)."
    case "∧.I" =>
      "∧.I — prove both halves.\n" +
        "           program: build a pair.  logic: to prove A ∧ B, prove A and prove B."
    case "∧.E₁" | "∧.E₂" =>
      "∧.E — take a pair apart.\n" +
        "           program: ._1 or ._2.  logic: from A ∧ B, conclude either half."
    case "∨.I₁" | "∨.I₂" =>
      "∨.I — commit to one side.\n" +
        "           program: Left or Right.  logic: to prove A ∨ B, prove one of them.\n" +
        "           Committing early is the classic mistake — you may not know which side holds."
    case "∨.E" =>
      "∨.E — case analysis.\n" +
        "           program: pattern match.  logic: to use A ∨ B, prove your goal twice,\n" +
        "           once assuming A and once assuming B."
    case "⊤.I" =>
      "⊤.I — trivially true.  program: ().  logic: ⊤ needs no evidence."
    case "⊥.E" =>
      "⊥.E — anything follows from a contradiction.\n" +
        "           program: an impossible value has no cases.  logic: ex falso quodlibet."
    case "Ax" =>
      "Ax — you already have one.\n" +
        "           program: use the variable.  logic: the goal is among your assumptions."
    case "let" =>
      "let — name something first, then carry on with it in scope.\n" +
        "           This is how you bring a value *into* scope before using it —\n" +
        "           needed when the thing you must take apart is buried inside something else."
    case other => other

  // --- ??? ------------------------------------------------------------------

  private def hint(goal: Goal, position: Partial): String =
    val offers = Moves.offers(position)
    val (safe, dead) = offers.partition(o => keepsAlive(o.move))

    // Whether the *position* is still winnable at all: every hole provable.
    val winnable = position.holes.forall((_, hole) => Decide.provable(hole))

    if !winnable then
      s"""${View.panel("hint")}
         |  This position cannot be finished — one of its holes is not provable.
         |  That is not necessarily your fault: the goal itself may be a
         |  non-theorem, in which case getting stuck everywhere *is* the answer.
         |
         |  The goal ${if Decide.provable(goal.formula) then "was provable at the start, so a wrong turn was taken — `back` to a choice point."
        else "is not an intuitionistic theorem. Exhausting the alternatives is the negative ending (§4.7)."}
         |""".stripMargin
    else
      val lesson =
        val committedEarly = dead.exists(o => NJ.label(o.move).startsWith("∨.I"))
        val inspectFirst = safe.exists(o => NJ.label(o.move) == "∨.E" || NJ.binds(o.move).isDefined)
        if committedEarly && inspectFirst then
          "\n  You are being offered Left and Right, and neither works yet.\n" +
            "  Inspect the disjunction before choosing a disjunct — find out which\n" +
            "  side you actually have, then commit."
        else ""

      // A `let` never loses the game — it adds to scope and leaves the goal
      // alone — so saying it "keeps the goal winnable" is true but weak advice.
      // Separating the two is more honest than a single list that mixes
      // progress with harmlessness.
      val (safeLets, safeMoves) = safe.partition(o => NJ.binds(o.move).isDefined)
      val letNote =
        if safeLets.isEmpty then ""
        else
          s"\n  also safe:          ${safeLets.map(_.number).mkString(", ")}" +
            "  (a let always is — it adds to scope\n" +
            "                      without touching the goal, so it can never lose)"

      s"""${View.panel("hint")}
         |  The goal is still winnable from here.
         |
         |  keeps it winnable:  ${if safeMoves.isEmpty then "(only lets — see below)" else safeMoves.map(_.number).mkString(", ")}
         |  leads nowhere:      ${if dead.isEmpty then "(none — anything works)" else dead.map(_.number).mkString(", ")}$letNote$lesson
         |
         |  Checked with the decision procedure, not by searching: a move is kept
         |  if every hole it opens is provable.
         |""".stripMargin

  /** Does this move leave every hole it opens provable?
    *
    * Sound because the rule is: if each opened sequent has a proof, they can be
    * assembled into one for the position. Terminating because LJT decides each
    * one — which is why hints are possible now and were not before.
    */
  private def keepsAlive(move: NJ[Sequent]): Boolean =
    NJ.subgoals(move).forall(Decide.provable)
