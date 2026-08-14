---
type: Roadmap
title: Roadmap
description: The big picture for curry-howard-game — where we are going and why.
tags: [roadmap, planning]
timestamp: 2026-08-14
---

# Roadmap

The big picture for `curry-howard-game`: where we are going and why. Narrative context, not a task list — concrete, actionable items live in [ToDo.md](ToDo.md).

## Goal

Build a game that teaches **logic** and **programming** simultaneously, by gamifying proof systems under the **Curry–Howard correspondence**. A single play is, at will, the construction of a natural-deduction proof or the type-driven development of a program: the player starts from one typed **hole** and fills it by applying **constructors** and **destructors** until no holes remain (positive win) or until every line of play is shown to fail (negative win). The full design is in [spec/specification.md](spec/specification.md).

"Done" for the first target: a playable game for **intuitionistic propositional logic** in Gentzen's natural deduction, i.e. the simply typed λ-calculus with products, coproducts, `Unit` and `Nothing` — with both viewpoints (proposition/proof and type/program) switchable in the interface, shipped as a web desktop application and a mobile application.

Beyond that target, the project is explicitly **not one game but many**: different logics (classical and intuitionistic propositional, first-order, second-order, linear, separation) and different proof systems for the same logic (natural deduction vs. sequent calculus), each corresponding under Curry–Howard to a different programming language.

## Approach

The formal core comes first and drives everything else. The §3 rule tables of the specification — natural-deduction rules on one side, constructors/destructors on the other — are the authoritative definition of legal moves; the game engine implements exactly those, and the UI is a rendering of a game state (open holes + variables in scope per hole) plus the legal moves available at it.

Two design commitments shape the work: the **two viewpoints must stay in lockstep** (one engine, two syntaxes, never two implementations that can drift), and the engine must be **parameterised by the calculus** from the start, so that adding a logic or a proof system means supplying rule tables rather than rewriting the core.

The knowledge layer follows the repository's OKF conventions ([AGENTS.md](AGENTS.md)): expensive analysis of a source is written once to a concept doc beside it and thereafter read from there.

## The big steps

### Step 1 — Complete the specification

Close the two stubbed sections of [spec/specification.md](spec/specification.md): §5 (logics and languages — for each supported logic and proof system, the corresponding language, the moves exposed to the player, and how each viewpoint is rendered) and §6 (platforms — technical and UX requirements for web desktop and mobile). Sections 1–4 (goal, rule tables, game mechanics, worked playthroughs) are written and serve as the reference for the rest.

### Step 2 — UI/UX design

Design how a game state is shown and manipulated: holes and their types, the scope attached to each hole, the palette of legal moves, choice points, backtracking, and the viewpoint switch between proposition and type. Mockups live in [doc/mockup/](doc/mockup/). Deferred: visual polish and platform-specific chrome until the interaction model is settled.

### Step 3 — Game engine for intuitionistic propositional logic

Implement the state, move and win machinery of §4 for the first calculus: typed holes with per-hole scopes, constructor/destructor moves (backward and forward use), legality checking against the §3 tables, backtracking over choice points, and cycle detection to bound infinite search trees. Deferred: other logics and proof systems.

### Step 4 — Playable application on the target platforms

Bring engine and UI together as a web desktop application and a mobile application, with a curated set of puzzles (the distributivity law, excluded middle, double-negation elimination — the worked examples of §4.9–4.10 — and a graded ladder beyond them).

### Step 5 — Generalise to further logics and proof systems

Add calculi behind the parameterised engine: sequent calculus for propositional logic, classical logic, first- and second-order, linear, separation. Each addition is a rule table plus its rendering in both viewpoints.

## Known gaps

- §5 (logics and languages) and §6 (platforms) of the specification are stubs marked "to be elaborated".
- No implementation exists yet — no engine, no UI, no platform scaffolding; the repository is design material only.
- The teaching notebooks under [doc/](doc/) (`3.4 Isomorphisms.ipynb`, `4.1 CurryHoward.ipynb`) have not been analyzed into concept docs, so what they contain beyond the specification is not yet recorded.
- Pedagogical design is untouched: puzzle progression, difficulty grading, hints, and how the negative win is taught are all open.
- Undecided: the implementation language/stack, and how much of the engine is shared between web and mobile.
- Open question: how far the two-player (dialogical / game-semantics) reading of §4.1 should surface in the actual game, rather than remaining a justification.
