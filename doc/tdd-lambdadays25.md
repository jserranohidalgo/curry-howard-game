---
type: Reference Implementation
title: hablapps/tdd — coalgebraic sequent calculi (lambdadays25)
description: A Scala 3 implementation of the LJ and LJT sequent calculi as (co)algebras, with tagless-final formulae and terms and a lazy proof-search space — assessed for reuse in the game engine.
resource: https://github.com/hablapps/tdd/tree/lambdadays25
tags: [engine, scala, sequent-calculus, ljt, proof-search, reuse, reference]
timestamp: 2026-08-15
---

# hablapps/tdd — coalgebraic sequent calculi (lambdadays25)

A testbed for "automating type-driven development with (co)algebras": the **LJ** and **LJT** sequent calculi implemented in Scala 3, with formulae and terms encoded tagless-final and proof search expressed as an unfold over a lazy search space. Roughly 1,800 lines, by the same author as this project, presented at Lambda Days 2025. Assessed here for what the game engine (Phase 2 of [Roadmap.md](/Roadmap.md)) can take from it.

The short version: **take the architecture, not the calculus.** The machinery around the rules is a better-founded version of what the game needs and what `doc/mockup/design/logic.jsx` hand-rolls; the rules themselves are for a different proof system than the one the game plays.

# Key concepts

- **A sequent is a hole.** `Sequent[F](ant: List[(Int, F)], con: F)` is exactly the game's notion of an open goal: `con` is the hole's type, `ant` is the resources in scope with their variable numbers, and `nextVar` is the fresh-name counter that `logic.jsx` reimplements as `freshName`. The game's state is a set of these plus the term being built around them.
- **A rule is a coalgebra plus an algebra.** `Rule[R]` splits every inference rule in two: `coalg: PartialFunction[Sequent[F], R[F, Sequent[F]]]` says *when the rule applies and which sub-goals it opens* (bottom-up, the search direction), and `alg: R[F, T] => T` says *how to assemble the proof term from the sub-terms* (top-down). Those are precisely the game's two needs — legal-move generation and growing the partial program — and the split is cleaner than the prototype's single `build()` closure.
- **Partiality is the applicability test.** A rule that does not match a sequent contributes nothing; `Sequent.rotations` re-presents the antecedent so that a left rule can fire on any premise in scope. That is the same generation strategy the game needs for "one destructor per in-scope variable".
- **The search space is a lazy final coalgebra.** `SearchSpace[F, C] = Mu[[t] =>> LazyList[C[F, t]]]` is the whole space of plays, unfolded on demand. `SearchSpace.trace` uses `Cofree` to keep the originating `Sequent` at every node — which is what the game's search-path panel displays.
- **Proofs are folds.** `Proof[F, C] = Mu[[t] =>> C[F, t]]`, and `proof.program` folds the calculus algebra over it to get the term. A finished game is a `Proof`; the term shown in the Play screen is that fold.
- **Search strategies are first-class.** `SearchStrategy` is a polymorphic function type with depth-first and iterative-deepening instances; the latter carries an explicit `MAX` depth, which is the honest way to bound a space that may not terminate.
- **Tagless-final `Form` and `Term`.** `Form[F]` gives the connectives plus a `fold`, with `Atom` / `False` / `Implies` / `And` / `Or` extractors for notation-independent matching; `Term[F, T]` gives `var`, `lam`, `inl`, `inr`, `match`, `and`, `_1`, `_2`, `apply`, `subst`. Two interpretations exist: `lambda.initial` (a plain ADT) and `lambda.scala3` (Scala 3 AST via quotes and staging).
- **LJT is Dyckhoff's contraction-free calculus.** `ljt/LJT.scala` implements the four left-implication rules that replace LJ's single non-terminating one, which is what makes proof search decidable for IPL.

# Relevance

## What the engine can take directly

| From the repo | Where it lands in the game |
|---|---|
| `Sequent` (+ `rotations`, `nextVar`) | the hole model: type + scope + fresh-variable supply |
| `Rule` (`coalg` / `alg`) | legal-move generation and partial-program construction, as data rather than closures |
| `Calculus` | the rule set as a single value — which is what "parameterised by the calculus" means in practice |
| `SearchSpace`, `Cofree` trace | the game tree: explored nodes with their sequents, lazily extended |
| `Proof`, `Mu.fold` | the finished term, and the natural-deduction rendering as another algebra |
| `SearchStrategy` (DF, iterative deepening) | later: hints, auto-solve, and bounded exhaustion |
| `Form` / `Term` tagless-final | the two notations as two interpreters over one structure |
| `Calculus[LJT]` | the reference decision procedure — see below |

Two of these solve problems the plan had already identified. **Moves as data**: the roadmap requires it for serializing the game tree (D4/D5), and `Rule`'s algebra/coalgebra split gives it for free where the prototype's `build()` closure does not. **One engine, two syntaxes**: the tagless-final encoding makes it structural rather than a discipline — instantiate `Show[F]` twice and the notation switch cannot drift.

**`Calculus[LJT]` is worth more than its line count.** Phase 6's exit criterion says the theorem/non-theorem corpus must be "cross-checked against a reference IPL decision procedure". This is that procedure, in-house. And the full negative ending — deferred past the first release by D13 — is exactly what LJT decides, so the deferred work becomes "wire in a calculus we already have" rather than "implement Dyckhoff".

## What does not transfer

- **The calculus is the wrong one for the game.** LJ and LJT are sequent calculi: left and right rules over Γ ⊢ G. The game (specification §3.2) plays *natural deduction*: constructors on a hole and destructors on a variable in scope, used **backward** (`x._1` fills a hole directly) or **forward** (`val qr = x._2` binds a new resource). The specification says as much in §2.4 — natural deduction and the sequent calculus "correspond to very different programming languages". So the game needs its own `Calculus[NJ]` instance; what it reuses is everything around it.
  - The gap is smaller than it first looks: LJ's *left* rules line up with the game's *forward* destructors (`LeftConjunction` ≈ `val (a, b) = p`, `LeftImplication` ≈ forward application). It is the backward destructors that are natural-deduction-only.
- **`tdd.lambda.scala3` cannot run in the browser.** `ScalaTerm` and `derivation` build a Scala 3 AST through quotes and staging, which needs a compiler at runtime — JVM only, and gated behind `-experimental`. No loss: the game renders Scala *source text*, so the `initial` ADT plus a printer is all it needs. The staging side stays useful on the JVM for generating test corpora.
- **`Show[Term]` is not the game's renderer.** It prints λ-calculus notation with numbered variables (`λ0: a. …`); the game needs Scala with readable names, and a natural-deduction derivation besides. Both are new algebras over the same term type.

## Consequences for the plan

1. **D10 needs amending.** The engine was specified as depending on nothing but the standard library; this architecture needs **cats** (`Traverse`, `Monad`, `Cofree`, `Alternative`). Cats cross-builds to Scala.js, so the cost is bundle size, not portability. `scala3-staging` remains excluded.
2. **Scala version.** The repo targets Scala 3.6 with `-experimental`; this project is pinned to 3.3 LTS. The calculus and `initial` layers should be fine on 3.3 — the `-experimental` flag is for the macro side. Confirm on first import; if the macro side is ever wanted, it belongs in a JVM-only module.
3. **Licence.** The repo is **CC BY-SA 4.0**, a share-alike content licence; this project is MIT. Same author on both, so nothing is blocked — recording the relicensing is a tidy-up before the repository goes public in Phase 9, not a precondition for reuse. (Creative Commons advise against CC licences for software, so the eventual fix is probably to move that repo off CC too.)
4. **Browser performance is unmeasured.** `rotations` allocates a sequent per premise per step, and `Mu`/`Cofree`/`LazyList` folding is allocation-heavy. Interactive move generation for a single hole is a small computation; the exhaustion search is where it would show. Measure at Phase 6 rather than guess.

# Citations

- <https://github.com/hablapps/tdd/tree/lambdadays25> — analyzed at `lambdadays25`, 2026-08-15. Author: Juan Manuel Serrano (URJC). Licence: CC BY-SA 4.0.
- `src/main/scala/tdd/calculus/{Calculus,Rule,Sequent,SearchSpace,Proof,SearchStrategy}.scala` — the reusable core.
- `src/main/scala/tdd/calculus/ljt/{LJT,Calculus}.scala` — Dyckhoff's contraction-free calculus.
- `src/main/scala/tdd/lambda/{Form,Term}.scala` and `lambda/initial/` — the tagless-final encodings and the ADT interpretation.
- `talks/lambdadays25/{TDD,TDD_Higher}.ipynb` — the accompanying talk material, not yet read.
- Related: [/doc/mockup.md](/doc/mockup.md) (the prototype engine this would replace), [/spec/specification.md](/spec/specification.md) §3.2 (the rule set the game actually plays).
