---
type: Roadmap
title: Roadmap
description: The phased workplan to a first working IPL application for curry-howard-game.
tags: [roadmap, planning, workplan, ipl]
timestamp: 2026-08-15
---

# Roadmap

The workplan to a **first working application for intuitionistic propositional logic (IPL)**. What the game is, what it is for, the principles behind it, and the assets it starts from are in [README.md](README.md); the granular, actionable items are in [ToDo.md](ToDo.md). This file is the plan itself.

Ten phases. Each names what it delivers, what it depends on, and how we know it is done. Phases 0–9 constitute the first application; work that deliberately falls outside it is listed under *Beyond the first application*. The plan is shaped by the fact that this is not a green-field build: an engine and a parser already run in the design handoff, and the UI is designed down to the token, so most phases are a port or a faithful re-implementation rather than an invention.

**Phases 1–9 are written against the provisional defaults recommended in Phase 0.** They are a plan for one reading of the design space, not the only one; closing Phase 0 differently revises them, which is exactly why it comes first.

## Phase 0 — Decide the design space

Nothing here is code. The specification fixes the *rules* of the game and the handoff fixes the *look*, but neither says what kind of software we are building — and several of the open questions change the architecture rather than merely the schedule. Answer them first, write the answers down, and only then scaffold anything.

Each decision below gives the options, a recommended default (so the plan has something to be written against) and what it changes downstream. A recommendation is a starting position, not a conclusion.

### Product shape and delivery

**D1 — Which platforms are in the first release?** *Options:* web desktop only · responsive web that also plays on a phone · web plus a separate mobile app. The specification names a web desktop app *and* a mobile app, but the handoff designs only the desktop layout, and the two-column Play screen has no phone design yet. *Recommendation:* web desktop for the first release, responsive down to a laptop, mobile after it. *Downstream:* the layout work in Phase 8, and whether Phase 9 includes app-store work at all.

**D2 — If mobile is in scope, is it a hybrid shell or a native client?** *Options:* a wrapper around the web app (Capacitor/PWA) · a second client sharing the engine (React Native, native) · a genuinely native rewrite. *Recommendation:* hybrid, reusing the engine untouched. *Downstream:* D8 (the engine's language must be reachable from the mobile client), and how much of Phase 8's UI can be shared.

**D3 — Where does it run, and is a server available at all?** *Options:* static hosting (GitHub Pages, URJC web space) with no backend · a university-hosted service with a backend. Also: should it be installable and playable offline (PWA)? Classrooms have unreliable wifi and the game needs no network to work. *Recommendation:* static, no backend, offline-capable. *Downstream:* this single answer settles the ceiling on D4–D7 — no backend means no accounts and no server-side collection.

### State and data

**D4 — Is the game stateful across sessions?** *Options:* (a) purely in-memory, a refresh restarts — what the prototype does; (b) local persistence: resume the game in progress, keep a history of goals solved; (c) server-side accounts with saved progress. *Recommendation:* (b), local only. *Downstream:* Phase 1's skeleton and, more importantly, a serialization requirement on the engine's state that is cheap in Phase 2 and expensive to retrofit.

**D5 — If state is saved, what exactly is saved?** *Options:* the current position only · the whole explored game tree · the tree plus a history of finished games. The tree *is* the teaching artefact — it is what shows the student their dead ends — and it is small JSON. *Recommendation:* the whole tree of the current game, plus a list of goals solved. *Downstream:* the engine's node representation must stay serializable (no closures in stored state — note that today's `Move` carries a `build()` function).

**D6 — Should goals and plays be shareable by URL?** Encoding the goal — and optionally the move list — in a link lets a teacher set an exercise and a student hand back a proof, with no accounts and no backend. *Recommendation:* yes for goals in the first release, plays after it. *Downstream:* needs the parser/printer round-trip that is already a Phase 3 exit criterion, plus replay of a move list in the engine.

**D7 — Does the course need to collect student work?** Grading, submission, per-student analytics. This is the one requirement that forces a backend, accounts, and a data-protection review — so it must be answered now even though the answer is probably "no". *Recommendation:* out of scope for the first application; revisit only with a concrete teaching need. *Downstream:* everything about D3–D5.

### Language and stack

**D8 — Which language for the engine?** *Options:* **TypeScript** — the handoff prototype is JavaScript, so the port is near-mechanical, and it runs everywhere the UI does · **Scala.js** — the game's object language *is* Scala and the teaching notebooks under [doc/](doc/) are Scala, so the engine could share code with the course material, at the cost of tooling friction on the UI side · something else entirely. *Recommendation:* TypeScript, unless sharing code with the notebooks is a real goal rather than an attractive one. *Downstream:* Phases 1–3 outright, and D2.

**D9 — Which UI framework?** *Options:* React (what the handoff is written in) · Vue/Svelte/other. The handoff insists its markup and CSS be rebuilt idiomatically whatever the choice, so this is not settled by the prototype — only the engine and parser are. *Recommendation:* React, to keep the port mechanical. *Downstream:* Phase 1, and the shape of Phases 5–8.

**D10 — One package or two?** *Options:* a single app with the engine inside it · separate `engine/` and `web/` packages. Keeping the engine independent is an approach commitment in [README.md](README.md) and it is what makes both the mobile client and the second logic cheap. *Recommendation:* two packages from the first commit. *Downstream:* Phase 1.

**D11 — What testing tooling?** The Phase 2 and Phase 3 exit criteria assume **property-based testing** (generate random types and move sequences, check invariants). Confirm the chosen language has it — `fast-check` for TypeScript, ScalaCheck for Scala — and that we intend to use it. *Recommendation:* yes; it is the cheapest guarantee that the engine only ever builds well-typed terms. *Downstream:* Phase 1's test setup; the exit criteria of Phases 2–3 as written.

### Scope of the first release

**D12 — Do both views ship, or programmer view first?** The correspondence *is* the product, which argues for both; the natural-deduction renderer is also the second-largest risk in the plan. *Recommendation:* both — but Phase 7 stays separable so it can slip without blocking a release.

**D13 — Does the negative ending ship?** Phase 6 is the schedule risk. *Options:* ship it · ship only the finite case (dead ends and finite exhaustion, which already work) and defer cycle detection. *Recommendation:* ship it, with the honest fallback that a search we cannot decide simply continues — the app must never falsely declare a goal refuted.

**D14 — EN and ES at launch, or EN first?** The handoff already carries both dictionaries. *Recommendation:* both.

**D15 — Free-form goals, a curated puzzle list, or both?** The handoff does both: a goal field plus five examples. A graded ladder and progression are explicitly deferred. *Recommendation:* both, with no progression in the first release.

### Institutional and non-functional

**D16 — Is URJC branding approved for this, and can we get the real assets?** The handoff's typefaces are documented substitutes and its atom and status colours are derived, because the *Manual de identidad visual* was unavailable; the logo files are also meant to be requested from the Dirección de Comunicación rather than reused. *Recommendation:* request both now — the swap is a one-file change if it arrives before Phase 8, and a re-do afterwards.

**D17 — Licence and repository home.** Open source (which licence?) or university-internal? It affects contribution, hosting, and which dependencies we may take. *Recommendation:* decide before the repository becomes public in Phase 9.

**D18 — Support matrix, accessibility target, and the deadline.** Which browsers and machines does the course actually use — lab desktops, student laptops, phones? What accessibility level do we commit to (WCAG 2.2 AA is the usual public-university baseline)? And is there a course date the release must hit? The academic calendar is a real scheduling input and belongs in the plan, not in someone's head. *Recommendation:* fix all three before Phase 1, since the deadline determines whether D12 and D13 stay as recommended.

*Depends on:* nothing. *Done when:* every decision above has an answer recorded — the product, state and scope answers (D1–D7, D12–D15) in [README.md](README.md) as goal and scope, the stack and institutional answers (D8–D11, D16–D18) in this file — and Phases 1–9 have been revised where an answer diverges from the default they were written against.

## Phase 1 — Foundations

Set up the project skeleton: an `engine/` package (pure, dependency-free, no UI imports) and a `web/` app, a test runner including the property-based tooling from D11, formatting and linting, and the URJC token layer brought in from `doc/mockup/design/urjc/tokens/` as the app's real token source — not as a second layer over legacy names, which the handoff explicitly warns against. Vendor the logo assets and the Lucide icon set properly (the prototype inlines icons only because its sandbox blocked remote CSS).

*Depends on:* Phase 0. *Done when:* the dev server serves an empty branded shell, the test command runs, and CI (if we want it) is green.

## Phase 2 — Engine core

Port `doc/mockup/design/logic.jsx`: the type model, term and hole model, per-hole scope, `legalMoves`, `applyMove`, `collectHoles`/`replaceHole`, `termStatus`, and the persistent game tree. Three deliberate changes during the port: **strip i18n out of the engine** (it currently calls `t()` while generating moves — emit structured move descriptors instead and translate at the edge), **keep the stored state serializable** if D4/D5 call for persistence (today a `Move` carries a `build()` closure, which cannot be saved), and add an **independent type checker** `check(term, ctx)` that the tests use to verify that every term the engine builds is well-typed. Fix the printer to re-sugar `A ⟶ ⊥` as `¬a` in logician notation, which the parser already accepts on input.

*Depends on:* Phase 1. *Done when:* the specification's §4.9 distributivity playthrough is reproduced move for move by a test; property tests generate random move sequences and the type checker accepts every resulting term; the move set matches §3.2 of the specification cell by cell, including the backward/forward distinction for `∧.E` and `⟶.E`.

## Phase 3 — Goal parser

Port `doc/mockup/design/parser.jsx`: both notations freely mixed, right-associative implication, negation desugaring to `A → ⊥`, positioned error messages, and `puzzleFromType` (collect the atoms of the parsed goal into the signature's type parameters). Pair it with the printer from Phase 2.

*Depends on:* Phase 2. *Done when:* a round-trip property holds in both notations — `parse(print(t))` ≡ `t` for generated types — and the five seeded examples parse to the intended goals in both notations.

## Phase 4 — Playable vertical slice

The first thing anyone can actually play: Home → Setup → Play wired end to end, programmer view only, minimal styling, no search-path panel and no negative ending. Select a hole, see the legal moves, apply one, watch the term grow, reach *won*.

*Depends on:* Phase 3. *Done when:* distributivity can be solved in the browser from a goal typed into Setup, and the win overlay appears. *Deferred:* everything below.

## Phase 5 — Search tree, backtracking and dead ends

The left column's *past*: the search-path tree (node rows with status dots, rule names, `Nh` / `✓ solved` / `✗ dead`, the current node marked by a red left bar), jump-to-node as the backtracking mechanism, Backtrack/Restart/Cancel with the single confirm dialog the design allows, and the **dead-end** terminal state as a toast that closes a branch rather than the game.

*Depends on:* Phase 4. *Done when:* the premature `∨.I` line of §4.9 can be played into a dead end, backtracked out of, and finished the productive way — all through the UI.

## Phase 6 — The negative ending

The hardest phase, and the one the prototype punts on. §4.7 of the specification requires a **negative win**: proving the goal uninhabited by exhausting the search. Today `nodeExhausted()` handles only the *finite* case (excluded middle), because forward projections and applications are always offered — so a player can keep binding values forever and the space is never exhausted — and because there is no detection of the **non-productive cycles** of §4.10 (double-negation elimination).

Two things close it: **bound the move space** (suppress forward moves that re-derive something already in scope; identify states up to goal type plus scope, so re-orderings are not distinct nodes) and **detect recurrence** (a node whose goal-and-scope already occurs on its own ancestor path is non-productive and can be cut). The literature to lean on is the contraction-free sequent calculus for IPL (Dyckhoff's LJT / Hudelmaier), which is exactly a terminating decision procedure for this problem — the design's constraint is that whatever we do must remain explicable to a student as *"this line goes in circles"*, not as an opaque oracle.

*Depends on:* Phase 5, and on D13. *Done when:* excluded middle is refuted by finite exhaustion and double-negation elimination by cycle detection, both ending in the "This is not a theorem!" overlay; a corpus of theorems and non-theorems is decided correctly, cross-checked against a reference IPL decision procedure; and no goal is ever *falsely* declared refuted — when the search cannot be decided, play simply continues.

## Phase 7 — The logician view

The second reading: the same term rendered as a Gentzen natural-deduction derivation — real fraction bars, rule names to the right of each bar, discharged assumptions bracketed with superscript labels, `let`-bindings inlined at their use sites so the derivation reads correctly. Plus the notation switch across goal, term, scope, rules table (*Construct/Destruct* → *Introduction/Elimination*) and prose, with a test that it never touches game state.

*Depends on:* Phase 4 (independent of 5–6, so it can run in parallel). *Done when:* the mid-game state of screenshot `04` renders as screenshot `05` from the same node, and switching view mid-play changes nothing but notation.

## Phase 8 — The designed UI

Recreate the handoff faithfully across every screen: Home (five systems, only IPL active, the rest tagged "Later"), Setup (goal field, live dual-notation echo, shape glyph, positioned errors, examples), Play (goal card, term card, hole chips with shape glyphs, resources-in-scope chips, and the rules table with its applicable / not-applicable / *no such rule* states, count badges and in-place instance unfolding), Help (six numbered steps illustrated with real UI components, plus the two-vocabulary glossary), and the Result overlay. Fixed table geometry, radius 0 on cards, no shadow at rest, motion at 80/140/200/320ms — and never an entrance animation that can leave content at `opacity: 0`.

*Depends on:* Phases 5–7. *Done when:* each screen matches its reference screenshot, and the rules table's geometry is provably stable across a whole game.

## Phase 9 — Release

EN/ES throughout — interface, engine-generated move labels, parser errors, and `document.documentElement.lang`. Keyboard and screen-reader support, focus rings, `Esc` dismissing but never confirming. The support matrix and accessibility target from D18. Then build, deploy, and run it with students.

*Depends on:* Phase 8. *Done when:* the app is deployed at a URL the course can use, an accessibility pass is clean, and it has survived one classroom session with the feedback written up here.

## Beyond the first application

- **Mobile.** The specification targets a mobile app as well as the web desktop app. The engine is already portable; what needs designing is the two-column Play screen on a phone, which the handoff does not cover.
- **More systems.** The Home screen already lists the four that follow: classical propositional logic (λμ-calculus, `call/cc`), first-order intuitionistic (λΠ), first-order classical, and multiplicative linear logic. Each is a rule table plus its rendering in both viewpoints — which is the test of whether the engine was really parameterised by the calculus.
- **Pedagogy.** Hints, per-move evaluation ("this returns you to essentially the same situation"), a graded puzzle ladder, scoring, and progression. Reserved in the layout, deliberately unbuilt.
- **Classroom features.** Whatever D6 and D7 defer: sharing a play rather than only a goal, and any form of submission or analytics.
- **Specification.** §5 (logics and languages) and §6 (platforms) still have to be written; Phase 0's answers are most of §6, and Phase 9's findings are the rest.

## Known gaps and risks

- **Phase 0 is unclosed, so Phases 1–9 are provisional.** They are written against the recommended defaults; a different answer to D1, D3, D4 or D8 changes the shape of the plan, not just its schedule.
- **Phase 6 is the schedule risk.** Everything else is a port or a re-implementation of something that already exists; the terminating decision procedure — and its presentation as a *teaching* artefact rather than an oracle — is genuine design work.
- **The ND derivation renderer (Phase 7) is the second risk:** the prototype has one, but layout of fraction bars, discharge labels and inlined bindings is fiddly and does not survive a naive port.
- **Brand assets are unresolved** (D16): substitute typefaces, derived colours, and logo files that should be requested rather than reused.
- **The notebooks under [doc/](doc/) are unanalyzed** — `3.4 Isomorphisms.ipynb` and `4.1 CurryHoward.ipynb` may hold puzzle material and framings worth pulling into Help and the example list.
- **Open question:** how far the two-player (dialogical / game-semantics) reading of §4.1 should surface in the game itself, rather than remaining a justification. It is the natural way to *explain* the negative ending to a student, which makes it relevant to Phase 6.
