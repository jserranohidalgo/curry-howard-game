# curry-howard-game

A game that teaches **logic** and **programming** at once, through the **Curry–Howard correspondence**: constructing a proof and inhabiting a type are the same activity, so a single play can be read either as *proving a proposition* or as *programming a signature*. It is intended for university courses on logic and declarative programming, and is delivered as a web desktop application and a mobile application.

## Goal

Gamify logical proof systems. The player starts from a single typed **hole** — a signature to inhabit, or equivalently a proposition to prove — and fills it by applying **constructors** and **destructors**, which are exactly the introduction and elimination rules of natural deduction. Play ends when no holes remain (a **positive win**: the program is complete and well-typed, the proof is finished) or when every line of play has been shown to fail (a **negative win**: the type is uninhabited, the proposition is not a theorem).

The project is ultimately **not one game but many**. There are many logics — classical propositional, first- and second-order, linear, separation — and several proof systems for the same logic, and under Curry–Howard each corresponds to a genuinely different programming language. Natural deduction and the sequent calculus for propositional logic are not notational variants; they are different languages.

The **near-term target is one of them**: a working application for **intuitionistic propositional logic (IPL)** ≡ the simply typed λ-calculus with products, coproducts, `Unit` and `Nothing`. The plan to get there is [Roadmap.md](Roadmap.md).

## Scope of the first release

Decided in Phase 0 of the workplan (2026-08-15); the stack and institutional decisions behind these are recorded in [Roadmap.md](Roadmap.md).

**A desktop web application, static and offline-capable.** It deploys as static files — GitHub Pages or URJC web space — with **no backend of any kind**, and it is installable and playable with no network, so classroom wifi cannot break a session. A mobile app comes after the first release: the specification names one, but the handoff designs only the desktop layout, and the two-column Play screen needs a phone design that does not exist yet.

**No accounts, no server, no collected work.** Game state is saved **locally**, and what is saved is the *whole explored game tree* — every branch, dead end and backtrack — so a student can close a laptop mid-proof and resume with the search intact. The tree is the teaching artefact, not a cache. Sharing goals or plays by link, and any form of submission, grading or analytics, are out of scope; the last of these would require a backend and a data-protection review, and would mean reopening the hosting decision.

**Both viewpoints, from the start.** The programmer view and the logician view ship together. The correspondence *is* the product, and a release with only one of them teaches half the lesson.

**Both languages, from the start.** Spanish and English across the interface, the engine's move labels and the parser's errors.

**Goals are typed or picked.** A goal field accepting either notation, plus a short list of curated examples — including one deliberate non-theorem, so a player can meet the negative ending on purpose. No difficulty ladder or progression.

**The negative ending ships only in its finite form.** Dead ends and full exploration of a *finite* search space both work, so excluded middle is refutable. Detecting the non-productive cycles that make a search infinite — the double-negation-elimination case — is deferred past the first release. The rule that survives either way: when a search cannot be decided, play simply continues. The app must never declare a goal refuted that has not actually been refuted.

## Approach

**The rule tables are the single source of truth.** §3 of [the specification](spec/specification.md) defines the legal moves; the engine implements exactly those, and every screen is a rendering of a game state — the open holes, and the resources in scope at each — plus the moves legal at it. Where an existing prototype and the specification disagree, the specification wins.

**One engine, two syntaxes.** The programmer and logician views must never become two implementations that can drift. The engine emits structured, notation-free descriptions; rendering to Scala or to a Gentzen derivation happens at the edge, as does translation to EN/ES.

**Engine first, and framework-free.** The engine is a pure module with no UI dependency, developed with its own tests. That is what makes the eventual mobile app, and the eventual second logic, cheap.

**Vertical slice early.** Get an ugly but genuinely playable game running end to end as soon as the engine and parser exist, before any branded UI work. It validates the port, and everything after it is improvement rather than integration.

**Leave room, build nothing.** Additional logics, hints, move evaluation, scoring, accounts and persistence are all out of scope for the first application. Structure for them — a list of one system on the home screen, a reserved slot in the left column — and build none of them.

## Where we stand

Three assets exist, and the workplan is largely a matter of joining them:

- **The rules** — [spec/specification.md](spec/specification.md) §§1–4: the two parallel rule tables (natural deduction ⟷ constructors and destructors of algebraic data types), the game mechanics (holes, moves, resources, dead ends, backtracking, both wins), and worked playthroughs including two negative ones. §5 (logics and languages) and §6 (platforms) are still stubs.
- **The design** — [doc/mockup/](doc/mockup/), analyzed in [doc/mockup.md](doc/mockup.md): a high-fidelity, URJC-branded handoff for the minimal IPL version (Home → Setup → Play, plus Help and a Result overlay), with an interaction spec and reference screenshots of every screen.
- **A working prototype** — inside that same handoff, `design/logic.jsx` and `design/parser.jsx` are a running engine and goal parser in plain JavaScript. The handoff is explicit that these two are to be **ported as logic, not re-derived from prose**; everything else in `design/` is presentation scaffolding to be rebuilt idiomatically.

So the first application is not a green-field build: it is a port, a hardening, and a faithful re-implementation of a UI that has already been designed down to the token.

## Playing it

**In a browser.** The app is Scala.js, so it is built to static files and served — there is no `web/run` (sbt would hand the linked JavaScript to Node, which has no DOM):

```sh
sbt distDev                                  # or distProd, optimised
python3 -m http.server 8080 --directory dist
open http://localhost:8080
```

`sbt "~distDev"` rebuilds on every change — a Scala source or anything under `static/` — and reloading the page picks it up. Two things worth knowing when it looks broken: **quote it**, since in zsh an unquoted `~distDev` is a tilde expansion that never reaches sbt; and sbt stamps sources by **hash**, so `touch` triggers nothing and only a real edit does.

**Why a server, when the app is static?** Only for the PWA half. Opening `dist/index.html` straight off the filesystem does play — the stylesheets, the tokens, the logos and `localStorage` all work from `file://` — but the origin is opaque, so the web manifest is refused as a cross-origin fetch and the service worker will not register. That is the offline shell D3 asks for, and it is also how the app will really be served, so the server is what we test against. (There is no single file to open, either: `dist/` is `index.html`, `js/main.js`, the stylesheets and the assets.)

**In a terminal.** The console version came first and goes further — it has the search path, backtracking and contextual help:

```sh
sbt repl/run
```

Type a signature (`(A, B) => (B, A)`) or the proposition it corresponds to (`a ∧ b → b ∧ a`) — either will do, and they may be mixed. A number plays a move; `?` says what to do at any prompt, `??` explains the moves on offer, `???` gives a hint. `back`, `view`, `tree`, `goal` and `quit` do the rest.

## Reading order

- [Roadmap.md](Roadmap.md) — the phased workplan to a first working IPL application.
- [ToDo.md](ToDo.md) — what is pending and what is done.
- [spec/specification.md](spec/specification.md) — the design: rules, mechanics, playthroughs.
- [doc/mockup.md](doc/mockup.md) — the design handoff and its prototype.
- [index.md](index.md) — the machine-navigable entry point. If you are an AI agent, start from [AGENTS.md](AGENTS.md).

This repository is organized as an [Open Knowledge Format](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md) bundle — see [AGENTS.md](AGENTS.md) for the conventions.

Released under the [MIT License](LICENSE).
