# Interaction Structure — Curry–Howard Game (minimal version)

Guidelines for the minimal, codeable version of the app. Scope: **one** game engine —
simply typed λ-calculus with algebraic data types ≡ propositional intuitionistic logic (IPL).
Everything else (linear, classical, first-order) is future work and must not shape the
minimal build beyond leaving room for it.

---

## 1 Principles

1. **One game, two readings.** Programmer view and logician view are the *same* game,
   never two modes of play. The view switch is available at *any* time, on every screen,
   and never alters game state.
2. **Two tenses, two sides.** The left region always shows the **past and the future**
   (explored search path + moves available next); the right region always shows the
   **present** (goal, term/proof under construction, holes, resources).
3. **Only legal moves are playable.** The rules table is always fully visible — it *is*
   the syllabus — but only applicable entries are actionable. Illegality is shown by
   de-emphasis, never by hiding.
4. **No dead ends without an exit.** Every terminal state offers exactly one obvious way
   onward (backtrack, or return to Home).
5. **Out of scope for now:** hints, move evaluation/commentary, scoring, puzzle libraries,
   accounts, persistence. Reserve the space, build none of it.

---

## 2 Screen map

```
     ┌─────────────────── Cancel* ─────────────────────────────────┐
     │                                                                  │
     │   ┌──────────┐   Start   ┌──────────┐   Begin   ┌──────────┐   │
     └──►│   HOME   │──────────►│  SETUP   │──────────►│   PLAY   │◄──┘
     ┌──►│          │◄──────────│          │           │          │
     │   └──────────┘   Back    └──────────┘           └──────────┘
     │     │   ▲                                                  │
     │     │   │ Back                                    terminal state
     │Help ▼   │                                                  ▼
     │   ┌──────────┐                                    ┌──────────┐
     │   │   HELP   │                                    │  RESULT  │
     │   └──────────┘                                    │ (overlay)│
     │                                                  └──────────┘
     └─────────────────── New game ─────────────────────────┘

  * Cancel requires confirmation (§6.1)
```

**HOME is the only hub.** Every exit leads back to it: **Back** from SETUP and from HELP,
**Cancel** from PLAY (confirmed), and **New game** from the RESULT overlay. There is no
way to move backwards from PLAY to SETUP — a new goal is always entered from HOME.

Three screens plus one overlay. Global to all of them: the **view switch**
(Programmer ⟷ Logician) and the theme toggle.

---

## 3 HOME

Minimal landing page. Contents, in order:

1. **Title** and a one-sentence statement of what the app is:
   *a game in which building a program and proving a theorem are the same move.*
2. **A short paragraph** (3–4 sentences max) on the Curry–Howard correspondence, phrased
   in the currently selected view's vocabulary.
3. **Start game** — primary action. One button only.
   - Labelled with the logic/language it plays: *Simply typed λ-calculus · IPL*.
   - Future logics appear as **additional sibling buttons** in this same list. The layout
     must already be a list of one, not a single centred button, so adding the second
     costs nothing.
4. **Help** — secondary action → HELP.

No other affordances. No hero art, no stats, no settings beyond the two global toggles.

---

## 4 HELP

A step-by-step guide to *how a game is played*. Static, scrollable, no interaction beyond
scrolling and **Back**. Structure it as numbered steps mirroring real play:

1. What a goal is (a signature to inhabit / a proposition to prove).
2. What a **hole** is — an unfinished obligation, with its type; a type has a *shape*.
3. What **resources** are — inputs and bindings available at the selected hole
   (hypotheses in scope).
4. What a **move** is — an introduction/elimination rule ≡ a constructor/destructor.
   Show the rules table and explain that dimmed entries are simply not applicable here.
5. **Choice points and backtracking** — the search path; how to return and try another
   branch.
6. **How a game ends** — the three terminal states of §6.
7. A short glossary table pairing the two vocabularies
   (type ↔ proposition, program ↔ proof, hole ↔ open goal, …).

Each step should be illustrated with a small still from the real UI rather than prose alone.

---

## 5 SETUP — entering the goal

Reached only via **Start game**. Its single job: obtain a well-formed goal.

- **One input field.** The user types a signature (programmer view) or a proposition
  (logician view). The field's placeholder, syntax hint and validation messages follow the
  current view; switching view **re-renders the same goal in the other notation** and must
  not clear the field.
- **Accepted grammar** (minimal):
  - atoms — `A`, `B`, `P`, `Q`, … (programmer) / `a`, `b`, `p`, `q`, … (logician)
  - `=>` / `→` implication (right-associative)
  - `(A, B)` / `A ∧ B` conjunction
  - `Either[A, B]` / `A ∨ B` disjunction
  - `Unit` / `⊤`, `Nothing` / `⊥`
  - parentheses
- **Live feedback.** As the user types, show (a) the parsed goal rendered in *both*
  notations, and (b) its shape glyph. Parse errors appear inline, non-modally, and point at
  the offending position. **Begin** stays disabled until the goal parses.
- **Examples.** A short row of clickable example goals that fill the field — enough to play
  immediately without inventing one. Include at least one non-theorem so a player can meet
  the negative ending on purpose.
- **Back** returns to HOME. No confirmation needed (nothing has been built yet).

---

## 6 PLAY — where the search happens

The screen where all play occurs. Two regions.

**Left — past and future**
- **Search path / tree**, collapsible. Shows every node explored so far, marks choice
  points, solved branches and dead ends, and allows jumping to any node (which *is*
  backtracking). Collapsed by default when the rules table needs the room.
- **Moves — the rules table.** Always shows the complete I/E table of IPL ≡ the
  constructors/destructors of the ADTs. For the selected hole:
  - applicable entries are emphasised and actionable;
  - inapplicable entries are dimmed but still legible;
  - an entry with several concrete instances shows a count and **unfolds in place** to let
    the user pick one;
  - a cell where no rule exists at all (e.g. ⊥-introduction) is rendered as an explicit
    absence, not an empty gap.
- **Backtrack** and **Restart**, always reachable.
- *Reserved, not built:* **Hint** (highlight moves likelier to reach the goal) and
  per-move **evaluation** (“great move!”, “this returns you to essentially the same
  situation”). Leave a slot in this region for both.

**Right — the present**
- The **goal**, in the current view's notation.
- The **program/proof constructed so far**, with holes shown as typed, shaped sockets.
  Exactly one hole is *selected* at a time; the moves table always refers to it.
- The **resources in scope** at the selected hole.

**Cancel** exits to HOME and is the only destructive action on this screen — see §6.1.

### 6.1 Cancelling
Cancel (and Restart) open a confirmation dialog: *“Abandon this game? The search you have
explored will be lost.”* with **Abandon** / **Keep playing**. Keyboard: `Esc` dismisses,
never confirms. No other action in the app requires confirmation.

### 6.2 Terminal states
Evaluated after every move; each shows a message over the board and offers the single
sensible exit.

| Condition | Verdict | Message | Exit offered |
|---|---|---|---|
| No holes remain | **Won** | *Solved — the proposition is proved / the type is inhabited.* | **New game → HOME** |
| Some hole has no applicable move | **Dead end** (branch only, not the game) | *No legal move here. Backtrack to a choice point.* | Backtrack (stays in PLAY) |
| Search space finite **and** fully explored with no solution | **Lost / refuted** | **“This is not a theorem!”** | **New game → HOME** |

The two game-ending verdicts (Won, Lost) offer **New game**, which returns to HOME — the
player enters the next goal from there. Only the dead end keeps the player in PLAY.

Note the distinction: a dead end closes a *branch* and is recoverable; exhaustion of the
whole finite search space ends the *game* automatically. Detecting the latter requires
tracking, per node, whether every legal move has been tried, and looping-state (cycle)
detection so that a search which can grow forever is never falsely declared exhausted —
when it cannot be decided, the game simply continues.
