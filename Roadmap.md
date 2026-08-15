---
type: Roadmap
title: Roadmap
description: The phased workplan to a first working IPL application for curry-howard-game.
tags: [roadmap, planning, workplan, ipl]
timestamp: 2026-08-15
---

# Roadmap

The workplan to a **first working application for intuitionistic propositional logic (IPL)**. What the game is, what it is for, the principles behind it, and the assets it starts from are in [README.md](README.md); the granular, actionable items are in [ToDo.md](ToDo.md). This file is the plan itself.

Ten phases. Each names what it delivers, what it depends on, and how we know it is done. Phases 0–9 constitute the first application; work that deliberately falls outside it is listed under *Beyond the first application*. The plan is shaped by the fact that this is not a green-field build: an engine and a parser already run in the design handoff, and the UI is designed down to the token, so several phases are a re-implementation from a precise specification rather than an invention.

**Phase 0 is closed** (2026-08-15); Phases 1–9 have been revised against its answers. The two that reshaped the plan most: the application is **Scala.js throughout**, so the handoff's JavaScript is a specification to transcribe rather than code to port; and the negative ending ships **only in its finite form**, which narrows Phase 6 and moves cycle detection past the first release.

## Phase 0 — The design space (decided 2026-08-15)

Nothing here was code. The specification fixes the *rules* of the game and the handoff fixes the *look*, but neither says what kind of software we are building — and several of the open questions changed the architecture rather than merely the schedule. This is the record of what was decided and why; the product, state and scope answers are also written up as [README.md](README.md)'s *Scope of the first release*.

### Product shape and delivery

- **D1 — Platforms.** **Web desktop first**, responsive down to a laptop; mobile after the first release. The specification names a mobile app too, but the handoff designs only the desktop layout and the two-column Play screen has no phone design.
- **D2 — Mobile client shape.** *Deferred with mobile itself.* The engine stays framework-free and is cross-built (D19), so both a hybrid shell around the web app and a separate client remain open.
- **D3 — Hosting.** **Static, no backend, offline-capable.** Deployable to GitHub Pages or URJC web space; installable and playable with no network, so classroom wifi cannot break a session. This answer sets the ceiling for D4–D7.

### State and data

- **D4/D5 — Persistence.** **Save the whole explored game tree, locally.** A student can close a laptop mid-proof and resume with every branch, dead end and backtrack intact — the tree is the teaching artefact, not a cache. Imposes one constraint on Phase 2: engine state must be serializable, so a move must be *data* rather than the `build()` closure the prototype carries.
- **D6 — Sharing by URL.** **Not in the first release.** Goals are typed into Setup or picked from the examples. A link-based way to set an exercise can be added later without rework, since it rides on the parser/printer round-trip.
- **D7 — Collecting student work.** **Out of scope**, by construction: D3 leaves no server to collect it. A real need for submission, grading or analytics means reopening D3 first, and brings a data-protection review with it.

### Language and stack

- **D8 — Language.** **Scala.js throughout.** The engine is an ADT interpreter, and sealed traits with exhaustive matching are a genuine correctness aid for exactly that — adding a type former makes the compiler enumerate every unhandled case. The object language rendered in the programmer view is Scala, and the notebooks under [doc/](doc/) are Scala. The cost, accepted knowingly: the handoff's JavaScript becomes a *specification to transcribe* rather than code to port, and the UI is written rather than ported.
- **D9 — UI library.** **Laminar.** Direct DOM, no virtual DOM, thin abstraction — which is what recreating a pixel-specified design with hand-written CSS needs. (Tyrian was the near miss: the handoff's state model is an Elm model verbatim.)
- **D10 — Packaging.** **Separate `engine/` and `web/`**, from the first commit, with the engine importing nothing from the UI. *Amended 2026-08-15 (see D20):* the engine was to depend on nothing but the standard library; it now also depends on **cats**, which the adopted architecture needs (`Traverse`, `Monad`, `Cofree`, `Alternative`). Cats cross-builds to Scala.js, so the cost is bundle size rather than portability. `scala3-staging` stays excluded — it needs a compiler at runtime and cannot reach the browser.
- **D21 — Keep the engine minimal (2026-08-15).** The first cut of Phase 2 carried four type classes — `Term`, `Rule`, `Calculus` and `Form` — inherited from the reference implementation. All four are gone. `Term` was strictly redundant: the NJ algebra is at exactly the right granularity, since every destructor acts on a *variable in scope*, so `Ax`, backward `∧.E₁` and the rest are fixed compositions of `Term` operations rather than needing the finer vocabulary — and `subst`, which the sequent-calculus algebras need, was never called. `Rule` was a `PartialFunction` wearing a type class once its algebra moved out. `Calculus` bundled a coalgebra, an algebra and a `Traverse` for a single instance. `Form` was the last to go: a tagless-final formula interface buys the ability to have *alternative representations* of a proposition, and the game has one. Dropping it takes every `[F: Form]` context bound with it, and structural equality — which the rules use constantly, to decide whether a resource's type *is* the goal — becomes the case classes' own `==`. Note what did **not** depend on it: the two readings of a formula were never two instances of a class, they are two functions over one ADT, so "one engine, two syntaxes" survives untouched. What remains: the `Formula` enum, the `NJ` enum, its coalgebra, a `Traverse`, and one file per interpretation. `Partial`, `GameTree` and the search plumbing are **specialised to NJ** rather than generic over the calculus — there is one game calculus, LJT is only ever consulted and never played, and the generality was costing real friction. `Mu` and the search space stay generic, so the oracle can still reuse them. The rule of thumb: reintroduce an abstraction when a second instance actually needs it.
- **D20 — Engine architecture.** **Adopt the coalgebraic structure of [hablapps/tdd](https://github.com/hablapps/tdd/tree/lambdadays25)**, analyzed in [doc/tdd-lambdadays25.md](doc/tdd-lambdadays25.md): sequents as holes, each rule as a `coalg`/`alg` pair, the search space as a lazily unfolded final coalgebra, and formulae and terms encoded tagless-final. It is better-founded than the prototype it replaces, and it settles two things the plan had to solve by hand — moves become *data* rather than closures (which is what D4/D5's serialization needs), and the two notations become two interpreters over one structure rather than two renderers kept in step. The game's own rule set is natural deduction, so the calculus instance is new; everything around it is reused. Its `Calculus[LJT]` also supplies the reference decision procedure Phase 6 needs and most of the deferred negative ending.
- **D11 — Testing.** **Property-based testing with ScalaCheck**, alongside example-based tests. It is the cheapest guarantee that the engine only ever builds well-typed terms.
- **D19 — Build layout.** **sbt with the engine cross-built to JVM and JS.** The property suites run on the JVM at full speed while the web app consumes the JS artifact — which matters most in Phase 6, where the search space gets probed hard.

### Scope of the first release

- **D12 — Views.** **Both**, from the start. The correspondence is the product.
- **D13 — Negative ending.** **Finite case only.** Dead ends and exhaustion of a *finite* search space ship, so excluded middle is refutable; cycle detection for infinite searches is deferred. The invariant that holds either way: an undecided search simply continues, and the app never declares a goal refuted that has not been refuted.
- **D14 — Languages.** **Spanish and English** at launch, across interface, move labels and parser errors.
- **D15 — Goals.** **Free-form entry and curated examples**, as the handoff designs — including one deliberate non-theorem. No difficulty ladder or progression.

### Institutional and non-functional

- **D16 — Branding.** **Ship with the documented substitutes**: Archivo / Source Sans 3 / Source Serif 4 / IBM Plex Mono, the derived atom and status colours, and the logo files as they arrive in the handoff. Faster, and accepted with its two risks — a rebrand pass if the *Manual de identidad visual* later contradicts the substitutes, and institutional assets used without sign-off from the Dirección de Comunicación.
- **D17 — Licence.** **MIT**, in [LICENSE](LICENSE). Short enough that a student can read it, and compatible with everything including GPLv2 — which Apache-2.0 is not. There is no plausible patent surface in a teaching game, so Apache-2.0's patent grant would have bought little for its extra weight. One thing still worth confirming: the copyright line names the author, but as university work the holder may be URJC — changing it is a one-line edit.
- **D18a — Accessibility and support.** **Not a goal for the first release.** No conformance target and no audit; current Chrome, Firefox, Safari and Edge are the support matrix. The keyboard behaviour the handoff specifies — `Enter` submits in Setup, `Esc` dismisses a dialog and never confirms — is design fidelity and still ships. Noted once for the record: if the app is hosted on URJC infrastructure, Spanish public-sector rules (RD 1112/2018 / EN 301 549) may impose a conformance level regardless of this decision.
- **D18b — Deadline.** **Thursday 10 September 2026**, the course start. The scope agreed in D12–D15 stands as answered; the plan is to keep both, measure progress, and reschedule if the work says so rather than pre-emptively cutting. See *Known gaps and risks* for the checkpoint that decides it.

*Done when:* every decision above has an answer recorded, and Phases 1–9 have been revised where an answer diverges from the default they were written against. **Both are done**, except for the release date under D18b and the licence choice under D17.

## Phase 1 — Foundations (done 2026-08-15)

**Built:** sbt 1.12.15 pinned in `project/build.properties` (the installed launcher is sbt 2.0.6, whose plugin ecosystem is not worth the schedule risk), on JDK 21 via a project-local `.sbtopts` so the machine's default Java is untouched. Scala 3.3.8 LTS. `engine` cross-builds to JVM and JS; `web` is Scala.js + Laminar 17.2.1 with munit and ScalaCheck on the JVM side. No npm and no bundler — the app has no JavaScript dependencies, so `distDev`/`distProd` assemble `dist/` from the linker output plus `static/`. Production bundle: 639 KB, 99 KB gzipped. Verified: JVM suites pass, the branded shell renders in a browser, and the app reloads byte-identically with the server stopped and the origin unreachable. Outstanding within the phase: scalafmt, optional CI, and the four typefaces (the design system `@import`s them from Google Fonts, which contradicts D3, so `static/styles/fonts.css` documents the gap and the shell falls back to system faces until the woff2 files are vendored in Phase 8).

The original plan for the phase:

An sbt build with two modules: `engine`, a cross-project in Scala 3 building for both the JVM and Scala.js, depending on nothing but the standard library; and `web`, a Scala.js application on Laminar. ScalaCheck and a unit-test framework wired into the JVM side of the engine, formatting and linting, and a bundler serving the Laminar app. The URJC token files from `doc/mockup/design/urjc/tokens/` come in as the app's real token source — not as a second layer over legacy names, which the handoff explicitly warns against. Vendor the logo assets and the Lucide glyphs as plain SVG (the prototype inlines them as JSX only because its sandbox blocked remote CSS). Add the licence file from D17, and stand up the offline/PWA shell early rather than bolting it on at the end — with a Scala.js bundle it is a service worker plus a cache manifest, and it is easier to keep working than to retrofit.

*Depends on:* Phase 0. *Done when:* the dev server serves an empty branded shell, `sbt test` runs the engine's JVM suites, the app builds to static files that load with the network switched off, and CI (if we want it) is green.

## Phase 2 — Engine core

Build the engine on the coalgebraic architecture adopted in D20, with `doc/mockup/design/logic.jsx` demoted from model to *specification of the rule set*: it says exactly which moves exist and what each one opens and binds, and §3.2 of the specification says the same thing more precisely. What the prototype does not supply — and what is taken from [hablapps/tdd](https://github.com/hablapps/tdd/tree/lambdadays25) instead ([doc/tdd-lambdadays25.md](doc/tdd-lambdadays25.md)) — is the shape of the thing.

The core, in the order it wants building:

- **`Sequent` as the hole.** Type plus the resources in scope plus a fresh-variable supply. One structure, reused for every open goal.
- **`Form` and `Term`, tagless-final.** The two notations become two interpreters over one structure. This is where "one engine, two syntaxes" stops being a discipline and becomes something the compiler enforces, and it is what Phase 7's derivation renderer will be written against.
- **A natural-deduction `Calculus[NJ]`** — the game's own rule set, since LJ and LJT are sequent calculi and the game plays natural deduction (specification §2.4 is explicit that these are different languages).

  *Why not drive the game from LJT and translate?* Because translation works on **finished proofs**, and the game is played on **partial ones**. Every move a player makes has to be a rule they picked off the table, and there is no move-by-move correspondence to borrow: LJT's four left-implication rules exist only to make search terminate and appear nowhere in §3.2; backward eliminations (`x._1` closing a hole directly — moves 6 and 10 of the §4.9 playthrough) have no sequent-calculus counterpart at all, being a context change followed by an axiom; and LJ's ∧L decomposes both components at once where the game offers `∧E₁` and `∧E₂` as separate table cells. Since the rules table *is* the interface, every mismatch would need a lossy mapping exactly where the design means to teach. NJ is not from-scratch work, though: its introduction rules, axiom and ⊥-elimination are LJ's right rules almost verbatim, its forward eliminations are LJ's left rules at a different granularity, and only the backward eliminations are genuinely new. LJT keeps the job it is better at — **deciding**, as the Phase 6 oracle and later the full negative ending. Two calculi, two jobs, which is also the sharpest test of whether the engine is really parameterised by the calculus. (If hints ever return to scope, turning an LJT proof into a suggested *next NJ move* is proof translation, and it is the hard direction — another reason they stay deferred.) Each rule is a `coalg`/`alg` pair: `coalg` says when the rule applies to a sequent and which sub-goals it opens — that is legal-move generation, with partiality doing the applicability test — and `alg` says how the term is assembled from its sub-terms. Both backward and forward destructor use, per §3.2.
- **The game tree over a lazy search space.** The full space of plays is a lazily unfolded final coalgebra; what the player has explored is a finite path-set through it, keeping the originating sequent at each node so the search-path panel has something to render.

Departures from the prototype, three of which the architecture now hands us rather than costing effort:

- **Moves are data, not closures** — a consequence of the `coalg`/`alg` split, and what D4/D5's serialization requires. The prototype's `build()` function is exactly what cannot be saved.
- **No i18n in the engine.** It currently calls `t()` while generating moves; emit structured move descriptors and translate at the edge.
- **Children keyed by the move that produced them.** The prototype appends a child per application, so replaying the same move from the same node yields two children for one (hole, move) pair — which is what makes exhaustion unsound in Phase 6. Key children by their pair from the start.
- **An independent type checker** `check(term, ctx)`, used by the tests to verify that every term the engine builds is well-typed rather than trusting move generation.

Also fix the printer to re-sugar `A ⟶ ⊥` as `¬a` in logician notation, which the parser already accepts on input.

One caution carried over from the analysis: the reused code's allocation behaviour in a browser is unmeasured — fine for one hole's moves, unknown for the Phase 6 search. (The licence mismatch is noted under *Known gaps and risks*; it does not gate this phase.)

*Depends on:* Phase 1. *Done when:* the specification's §4.9 distributivity playthrough is reproduced move for move by a test; ScalaCheck generates random goals and move sequences on the JVM and the type checker accepts every resulting term; a round-trip property holds over serialization of the game tree; and the move set matches §3.2 of the specification cell by cell, including the backward/forward distinction for `∧.E` and `⟶.E`.

## Phase 3 — Goal parser

Re-implement `doc/mockup/design/parser.jsx` in the engine module: both notations freely mixed, right-associative implication, negation desugaring to `A → ⊥`, positioned error messages, and `puzzleFromType` (collect the atoms of the parsed goal into the signature's type parameters). Pair it with the printer from Phase 2. Errors carry a position and a code, not a translated string — D14 means the message is chosen at the edge.

*Depends on:* Phase 2. *Done when:* a ScalaCheck round-trip property holds in both notations — `parse(print(t))` ≡ `t` for generated types — and the five seeded examples parse to the intended goals in both notations.

## Phase 4 — Playable vertical slice

The first thing anyone can actually play: Home → Setup → Play wired end to end in Laminar, programmer view only, minimal styling, no search-path panel and no negative ending. Select a hole, see the legal moves, apply one, watch the term grow, reach *won*. This is also where the engine's shape meets a real UI for the first time, so expect to revise the descriptors Phase 2 emits.

*Depends on:* Phase 3. *Done when:* distributivity can be solved in the browser from a goal typed into Setup, and the win overlay appears. *Deferred:* everything below.

## Phase 5 — Search tree, backtracking, dead ends and persistence

The left column's *past*: the search-path tree (node rows with status dots, rule names, `Nh` / `✓ solved` / `✗ dead`, the current node marked by a red left bar), jump-to-node as the backtracking mechanism, Backtrack/Restart/Cancel with the single confirm dialog the design allows, and the **dead-end** terminal state as a toast that closes a branch rather than the game.

Once the tree exists it is also what D4/D5 says to save, so local persistence lands here: serialize the game on change, restore it on load, and offer to resume or start fresh. Restart and Cancel must clear it, since the design treats both as abandoning the search.

*Depends on:* Phase 4, and on Phase 2's serializable state. *Done when:* the premature `∨.I` line of §4.9 can be played into a dead end, backtracked out of, and finished the productive way — all through the UI; and a mid-game reload comes back with the whole explored tree and the same current node.

## Phase 6 — The negative ending, finite case

§4.7 of the specification requires a **negative win**: proving the goal uninhabited by exhausting the search. Per D13 the first release ships only the *finite* half of that — the half where the search tree can actually be exhausted, which is enough for excluded middle and for the whole family of goals with no function or product in scope to keep feeding forward moves.

What this phase is really about is **soundness**, not reach. The prototype's `nodeExhausted()` decides "everything has been tried" by comparing a node's child count against the number of (hole, move) pairs available at it. Nothing prevents the same move being applied twice from the same node — jump back and replay, and one pair has two children — so the count can reach the threshold with pairs still untried, and the app announces "This is not a theorem!" about a goal that may well be one. Phase 2 keys children by their pair; this phase makes exhaustion read that keying, and tests it against the case that breaks the counting version.

The reduced scope has one visible consequence to design for: a goal whose search space is infinite never reaches a verdict, and simply keeps playing. That has to feel like an open game rather than a bug — which is the honest presentation of an undecided search, and the seam where cycle detection will later slot in.

The reference decision procedure the exit criterion calls for is no longer a thing to find: D20 brings in a working `Calculus[LJT]`, so the corpus can be cross-checked against Dyckhoff's contraction-free calculus running in the same engine.

**Measured during Phase 2, and it raises the stakes here.** Searching the NJ rule set does not merely risk non-termination in the double-negation case — it is unusable for almost anything. Asking whether `… : Q` is provable from `pqr: P ∧ (Q ∨ R)` — a hole three moves into the §4.9 playthrough — exhausted an 8 GB heap after 29 minutes at depth 8. The cause is structural: every *forward* move adds a resource to the scope, which multiplies the forward moves available at the next step, so the branching factor grows without bound rather than the depth merely being unbounded. Consequences: the NJ search is fine for shallow goals and useless past them, so **any question of the form "is this provable?" has to go to LJT**, whose whole purpose is to terminate. That makes the oracle load-bearing rather than a convenience, and it means bounding the NJ move space is about making the *game tree* finite, not about making search fast. Note the asymmetry that keeps this off the critical path for now: generating the moves at a single hole is one step and costs nothing, so **playing** is unaffected — it is only searching that blows up.

*Depends on:* Phase 5, and on D13. *Done when:* excluded middle reaches the "This is not a theorem!" overlay by genuine exhaustion; replaying a move from an earlier node can never produce a false refutation, with a regression test for exactly that; and a corpus of small theorems and non-theorems is decided or left open correctly, cross-checked against LJT. Double-negation elimination is expected to remain open — that is deferred work, not a failure.

## Phase 7 — The logician view

The second reading: the same term rendered as a Gentzen natural-deduction derivation — real fraction bars, rule names to the right of each bar, discharged assumptions bracketed with superscript labels, `let`-bindings inlined at their use sites so the derivation reads correctly. Plus the notation switch across goal, term, scope, rules table (*Construct/Destruct* → *Introduction/Elimination*) and prose, with a test that it never touches game state.

*Depends on:* Phase 4 (independent of 5–6, so it can run in parallel). *Done when:* the mid-game state of screenshot `04` renders as screenshot `05` from the same node, and switching view mid-play changes nothing but notation. Per D12 this ships in the first release, but keeping it independent means it can slip without blocking the phases around it.

## Phase 8 — The designed UI

Recreate the handoff faithfully across every screen, in Laminar and hand-written CSS: Home (five systems, only IPL active, the rest tagged "Later"), Setup (goal field, live dual-notation echo, shape glyph, positioned errors, examples), Play (goal card, term card, hole chips with shape glyphs, resources-in-scope chips, and the rules table with its applicable / not-applicable / *no such rule* states, count badges and in-place instance unfolding), Help (six numbered steps illustrated with real UI components, plus the two-vocabulary glossary), and the Result overlay. Fixed table geometry, radius 0 on cards, no shadow at rest, motion at 80/140/200/320ms — and never an entrance animation that can leave content at `opacity: 0`. Per D16 this is built on the substitute typefaces and derived colours as shipped.

*Depends on:* Phases 5–7. *Done when:* each screen matches its reference screenshot, and the rules table's geometry is provably stable across a whole game.

## Phase 9 — Release

Spanish and English throughout, per D14 — interface, engine-generated move labels, parser errors, and `document.documentElement.lang`. The keyboard behaviour the design specifies (`Enter` submits, `Esc` dismisses and never confirms) as part of design fidelity, not as an accessibility commitment: per D18a there is none. Then build to static files, deploy, and run it with students.

*Depends on:* Phase 8. *Done when:* the app is deployed at a URL the course can use, it loads and plays offline, the repository is public under [LICENSE](LICENSE), and it has survived one classroom session with the feedback written up here.

## Beyond the first application

- **The negative ending in full** (deferred by D13). Bounding the move space — suppressing forward moves that re-derive something already in scope, and identifying states up to goal type plus scope so re-orderings are not distinct nodes — and detecting recurrence, so that a node whose goal-and-scope already occurs on its own ancestor path can be cut. Dyckhoff's contraction-free calculus for IPL is exactly a terminating decision procedure for this, and D20 means we **already have an implementation of it** — so the work shrinks from "implement LJT" to "decide how a terminating search is presented to a player". That remaining part is the real design problem, and the constraint on it is unchanged: it must stay explicable to a student as *"this line goes in circles"*, not as an opaque oracle.
- **Mobile** (deferred by D1, shape left open by D2). The engine is cross-built and framework-free, so it travels; what needs designing is the two-column Play screen on a phone, which the handoff does not cover.
- **More systems.** The Home screen already lists the four that follow: classical propositional logic (λμ-calculus, `call/cc`), first-order intuitionistic (λΠ), first-order classical, and multiplicative linear logic. Each is a rule table plus its rendering in both viewpoints — which is the test of whether the engine was really parameterised by the calculus.
- **Pedagogy.** Hints, per-move evaluation ("this returns you to essentially the same situation"), a graded puzzle ladder, scoring, and progression. Reserved in the layout, deliberately unbuilt.
- **Classroom features.** Whatever D6 and D7 defer: sharing a play rather than only a goal, and any form of submission or analytics.
- **Specification.** §5 (logics and languages) and §6 (platforms) still have to be written; Phase 0's answers are most of §6, and Phase 9's findings are the rest.

## Known gaps and risks

- **The schedule is tight against 10 September 2026** — 19 weekdays from the plan being written, across the August holiday period, for ten phases starting from no code. The scope of D12–D15 is deliberately kept rather than pre-cut, and the date is revisited on evidence. **The checkpoint is Phase 4**, the playable vertical slice: it is the first honest measure of velocity, since reaching it exercises the build, the engine, the parser and Laminar together. If Phase 4 is not closing by the end of August, the choice is between moving the date and falling back to one of the reduced options — a plain UI over the URJC tokens with fewer components and states, or shipping the handoff's own prototype (vendored to run offline) for the first class while the Scala.js build continues behind it.
- **The ND derivation renderer (Phase 7) is now the largest technical risk**, since D13 took the decision procedure out of the first release. The prototype has one, but the layout of fraction bars, discharge labels and inlined bindings is fiddly, and it is being rebuilt in Laminar rather than ported.
- **Scala.js is the schedule unknown** (D8). The engine gains from it; the UI does not, and the handoff's screens are being written rather than ported. The Phase 4 vertical slice is deliberately early because it is the cheapest way to find out how far off that estimate is.
- **The reused engine code carries a different licence** (D20). [hablapps/tdd](https://github.com/hablapps/tdd/tree/lambdadays25) is CC BY-SA 4.0 and this project is MIT. Both are the same author's work, so nothing is blocked by it; putting the relicensing on the record is a Phase 9 tidy-up, before the repository goes public.
- **The reused code's browser cost is unmeasured** (D20). Sequent rotations plus lazy-list and fixpoint folding allocate freely. Generating one hole's moves is small; the Phase 6 search is where it could bite. Measure at Phase 6 rather than guess now.
- **Exhaustion is unsound until Phase 2 and Phase 6 land together.** The prototype can announce "This is not a theorem!" about a theorem if a move is replayed from an earlier node. Shipping the finite negative ending without the pair-keyed children fix would put a false refutation in front of students.
- **Brand assets are accepted as substitutes** (D16), with two open risks: a rebrand pass if the *Manual de identidad visual* later contradicts the typefaces and derived colours, and institutional logos used without sign-off from the Dirección de Comunicación.
- **Accessibility is out of scope by choice** (D18a). The only exposure is if URJC hosting turns out to carry a public-sector conformance obligation, which would be a constraint from outside the plan rather than a change of mind within it.
- **The notebooks under [doc/](doc/) are unanalyzed** — `3.4 Isomorphisms.ipynb` and `4.1 CurryHoward.ipynb` may hold puzzle material and framings worth pulling into Help and the example list.
- **Open question:** how far the two-player (dialogical / game-semantics) reading of §4.1 should surface in the game itself, rather than remaining a justification. It is the natural way to *explain* the negative ending to a student, which makes it relevant to Phase 6.
