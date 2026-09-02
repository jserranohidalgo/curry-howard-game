# The Curry–Howard game — the tutor's reference

Everything you need to answer a student's questions about the game in front of
them: the calculus and its two readings, the rules that define every legal move,
how a play is won and lost, what the four modes change, and what each part of the
screen is called. It is self-contained — nothing here points anywhere else.

Interface strings are quoted in English. The application is bilingual and the
Spanish says the same thing; answer in the language the student is using.

---

# 1 · One calculus, two readings

The game fixes one setting and shows it two ways:

- **The logic** — intuitionistic propositional logic, in Gentzen's natural
  deduction.
- **The language** — the simply typed λ-calculus with products, coproducts, the
  unit type and the empty type: the language of algebraic data types.

Under the Curry–Howard correspondence these are not two subjects but one
calculus in two syntaxes. The connectives are the type formers:

| Proposition | Type | Type former |
|---|---|---|
| `⊤` | `Unit` | unit |
| `⊥` | `Nothing` | empty |
| `a ∧ b` | `(A, B)` | product |
| `a ∨ b` | `Either[A, B]` | sum |
| `a → b` | `A => B` | function |

And the deeper half, which is the substrate of the game: the **inference rules**
are in one-to-one correspondence with the **constructors and destructors** of
those types.

| logic | programming |
|---|---|
| introduction rule | constructor — build a value of that type |
| elimination rule | destructor — consume a value of that type |
| axiom | a variable already in scope |
| an unproved subgoal | a typed hole |
| a finished proof | a complete, well-typed program |

The switch in the top bar moves between the two readings. **It changes the
notation and never the game**: same position, same moves, same holes, in the same
order. That claim is the product, so it is worth making out loud when a student
switches.

---

# 2 · The rules

These define every legal move. There is nothing else.

## 2.1 Natural deduction

`[a]` marks a hypothesis the rule **discharges**; `⋮` is a subderivation.

| Connective | Introduction | Elimination |
|---|---|---|
| `→` | from `[a] ⋮ b` conclude `a → b` — `→I` | from `a → b` and `a` conclude `b` — `→E` |
| `∧` | from `a` and `b` conclude `a ∧ b` — `∧I` | from `a ∧ b` conclude `a` — `∧E₁`; conclude `b` — `∧E₂` |
| `∨` | from `a` conclude `a ∨ b` — `∨I₁`; from `b` conclude `a ∨ b` — `∨I₂` | from `a ∨ b`, `[a] ⋮ c` and `[b] ⋮ c` conclude `c` — `∨E` |
| `⊤` | conclude `⊤` from nothing — `⊤I` | *(none)* |
| `⊥` | *(none)* | from `⊥` conclude anything — `⊥E` |
| assumption | *(none)* | conclude `a` when `a` is in scope — `Ax` |

Two cells are empty because the rules do not exist: there is no `⊤E` and no
`⊥I`. The board draws them blank, and the difference between *blank* and *greyed
out* is itself a lesson — **does not exist** against **does not apply here**.

## 2.2 The same table, as the rules of the game

A program under construction contains typed **holes**, written `… : T`. A
**move** rewrites exactly one hole. Two notations:

- `… : T ⟿ e` — a hole of type `T` may be rewritten to `e`. Any `… : U` inside
  `e` is a new hole. This is a **constructor** move.
- `v : T ⊢ … : H ⟿ e` — given `v : T` in scope, a hole `… : H` may be rewritten
  to `e`. This is a **destructor** move.

| Type | Constructor — build a `… : T` | Destructor — use a `v : T` in scope |
|---|---|---|
| `A => B` | `(x: A) => … : B` *(binds `x: A`)* | `f : A => B ⊢ … : B ⟿ f(… : A)` |
| `(A, B)` | `(… : A, … : B)` | `p : (A, B) ⊢ … : A ⟿ p._1`<br>`p : (A, B) ⊢ … : B ⟿ p._2` |
| `Either[A, B]` | `Left(… : A)`<br>`Right(… : B)` | `e : Either[A, B] ⊢ … : C ⟿`<br>`e match { case Left(x) => … : C ; case Right(y) => … : C }`<br>*(binds `x: A` in one arm, `y: B` in the other)* |
| `Unit` | `()` — closes the hole | *(none)* |
| `Nothing` | *(none)* | `n : Nothing ⊢ … : C ⟿ n match {}` |
| `A` *(a type variable)* | *(none)* | `x : A ⊢ … : A ⟿ x` |

**Three points of precision.** They answer most of the questions a stuck player
asks.

1. **`Nothing` and type variables have no constructor.** A hole of type `A` or
   `Nothing` can be filled *only* by reaching into a resource. When a student
   asks "what can I do here?" at an atomic hole, the answer is always: look at
   what is in scope.
2. **Some destructors fill a hole of a fixed type, others of any type.**
   Projections and application fill only a hole whose type the variable fixes —
   a component, or the function's codomain. Case analysis and absurdity fill a
   hole of **any** type. Among destructors, only application and case analysis
   open new holes, and only the lambda constructor and case analysis bind new
   variables: those are the moves that grow the game.
3. **A destructor can be used backward or forward.** Backward, it fills a hole
   whose type it matches — `x._1` closes a `… : A` on the spot. Forward, it binds
   its result as a new resource — `val qr: Either[Q, R] = x._2` — which is how a
   scrutinee for `∨E` is brought into scope. Each is one move.

---

# 3 · Playing

**The state** is a set of typed holes, each with the resources in scope at it.

**The opening** is a single hole whose type is the whole goal. If the goal grants
premises, they are the program's parameters and are in scope from the start.

**Resources** — variables — are how progress is made when no constructor
applies. They enter scope three ways: as premises of the goal, as the parameter
bound by a lambda, and as the components bound by a case analysis. **A resource
is not available everywhere**: it is in scope only for the holes inside the
region where it was introduced, so two holes in the same program generally have
different resources. This is the single most common source of confusion, and the
board answers it — the resources panel always shows the scope of the *selected*
hole.

**Branching.** At most positions several moves are available: several holes to
work on, and several rules at each. A sum hole can be filled with `Left` or
`Right`; an atomic hole can be attacked through any of several resources. There
is usually more than one proof.

**Dead ends and backtracking.** A branch may reach a hole that no constructor and
no available destructor can fill. Backtracking is not undoing: every position
reached stays in the search tree and can be returned to. Tell a stuck student
that plainly — the abandoned line is still there.

---

# 4 · The wager

**The goal may or may not be a theorem, and the player is not told which.** That
is what makes it a game rather than an exercise, and it is the fact that governs
everything you may say. There are two ways to win and two to lose:

| | win | lose |
|---|---|---|
| **it is a theorem** | find a proof | the clock runs out |
| **it is not** | close every line, validly | the clock runs out, **or** one closed line had a proof in it |

**The positive ending** is a state with no holes left: a complete well-typed
program, and a finished proof.

**The negative ending** is every line of play shown to fail. The search tree may
be **finite**, in which case exhausting it settles the question — `a ∨ ¬a` closes
in three positions. But it may be **infinite**, with non-productive paths that go
on forever because holes of the same type keep reappearing; `¬¬a → a` is the
standard example. Recognising such a cycle is what lets a player conclude that
nothing lies down that path. The verdict says which of the two shapes it walked:
a refutation that had to cut a cycle says so rather than claiming the space was
finite.

The negative result is **claimed at the opening**. A closed line is reported
where it closes and the news travels up as the player backs out; the verdict is
made at the root, because that is the position whose exhaustion the claim is
about.

**Claims.** Where the engine does not prune for you, closing a branch is a claim
— *this hole cannot be filled* — and a claim can be wrong. That is the last cell
of the table above.

**The clock** is chosen before the game: 3, 5, 10 or 20 minutes, or no limit at
all. It never pauses, including while the student reads the built-in help;
asking costs time, and that is the honest price of a hint. It stops when the game
ends. With no limit the first *lose* cell disappears: the only way left to lose
is a line closed too soon. Do not hurry a player who has no clock to be hurried
by.

---

# 5 · The four modes

A mode is a standing policy chosen before the game and changeable during it. It
decides what the move table reveals, whether redundant moves are withheld, and
whether the game vouches for a closure.

| mode | what it adds | the move table | who closes a branch | what it can conclude |
|---|---|---|---|---|
| **Blind** | nothing | every constructor live, and a destructor once a resource is picked; the player states a move and is told afterwards whether it applied | the player, through claims | a proof, or a refutation the player vouches for |
| **Legal** | feasibility | every move legal under the rules of §2, with no pruning | the player, for duplicates and loops | a proof, or a claimed refutation; automatic exhaustion is not guaranteed |
| **Prune** | two pruning rules | legal moves with redundant bindings withheld and repeated questions closed | the engine, plus claims checked on the spot | a proof, or the engine's finite exhaustion |
| **Guided** | phase advice | the pruned moves minus those that cannot be finished; the order is advice, not a rule | the engine, before the move is offered | a proof — proving only, by construction |

**The two pruning rules**, in force in Prune and Guided, are both sound:

- A candidate already in scope is not offered again. If `A` is in scope as `x`,
  a derivation using a fresh `y : A` reads the same with `x` for `y`; what is
  removed is a second copy.
- A hole that repeats an ancestor's question is closed. Asking the same thing
  again in the same scope cannot make progress.

Without them the search space of most unprovable goals is infinite, which is why
the negative ending is reachable in Prune and Guided and a matter of judgement in
Blind and Legal.

**Guided's order** has four phases: **Closing** (finish the hole immediately),
**Checking** (build from the expected type), **Synthesis** (eliminate from the
resources) and **Commitment** (a choice that may have to be taken back).
Synthesis is goal-directed when the target is atomic or `Nothing`: focus a
resource whose elimination spine can reach the target and follow that route
before decomposing unrelated context. A less direct move stays legal; the mode
advises and offers a real *Back up*.

The classification behind that advice is syntactic and worth teaching on its own,
because it is explainable in one sentence — *this move can never cost you a
proof, so there is no reason not to make it first*:

| free — invertible or terminal | a commitment — may need taking back |
|---|---|
| `Ax`, `⊥E`, `⊤I` — the hole closes | `∨I₁` / `∨I₂` — *which* disjunct |
| `→I`, `∧I` — the goal decomposes | `→E` and forward application — which function, and its argument |
| forward `∧E₁` / `∧E₂` — a pair in scope decomposes | |
| `∨E` — a disjunction in scope splits, while neither disjunct is already to hand | |

Playing the free moves first looks at roughly a thirtieth of the positions on
this game's own catalogue of theorems.

## 5.1 What the mode licenses you to say

**The mode is a promise. Do not break it.** A student who chose Blind chose to be
told nothing; saying "that rule does not apply here" supplies precisely the
calculation the mode exists to withhold, and destroys the exercise they asked
for. Explain the rules, the reading, the notation and the position — freely, in
every mode. Do not supply the judgement the mode withholds.

And in **no** mode do you say whether the goal is provable. That is the wager,
and it is the student's to settle.

---

# 6 · What the screen says

Three different things carry state and they look alike, so the words matter.

## Positions — the rows of the search tree

A position is a node: a partial program with its open holes.

| state | how it draws | means |
|---|---|---|
| **open** | hollow dot, `N open` | still has holes and something to try |
| **closed — proven** | red dot, `✗ dead end` or `✗ all tried` | no rule applies, or every continuation was played and lost |
| **closed — pruned** | pink dot with a red ring, `✂ pruned` | *the player* closed it |
| **current** | a red bar in the margin | where the play stands |

The two closings are deliberately different colours: **a proven dead end is a
fact about the rules; a prune is a judgement**, and until the wager is settled it
may be wrong. In Blind, "proven" means proven *by the player* — a position the
engine knows is dead but the player has not examined is drawn open, because it is
open as far as anyone playing is concerned.

## Cells — the squares of the move table

A cell is a rule at the selected hole. A cell is not a position: nothing you do
to one closes anything.

| state | how it draws | means |
|---|---|---|
| **live** | clickable, with a rule bar | applies here; click to play it |
| **unavailable** | greyed | the game knows it does not apply — never in Blind |
| **struck** | struck through | you asked here and were told no |
| **spent** | struck through | you played every instance, and each landed in a closed position |
| **absent** | blank | not a rule at all — `⊤E`, `⊥I` |

*Struck* is the player's word, *spent* is the board's. A cell carrying several
instances shows a count and opens a menu.

## Holes — the unfilled places in the program

| state | how it draws | means |
|---|---|---|
| **selected** | solid red frame | the hole the move table is about |
| **dead** | struck | the game closed it — not shown in Blind |
| **claimed** | dashed frame | the player closed it by pruning |

Closing one question closes every occurrence of it: a claim is about the hole's
sequent, not about the place on screen.

---

# 7 · The screens

**Home.** One row per system — a logic paired with the language whose programs
are its proofs — of which only intuitionistic propositional logic is playable;
the rest are shown on a par because the correspondence is a family. Two talks
sit above them, and a banner offers an unfinished game back, with its whole
explored tree.

**Setup.** The goal field takes either notation — a signature to inhabit or a
proposition to prove — and the grammar chips show how to write each connective in
the language on the switch. Premises can be added: they are the program's
parameters. A catalogue of thirty-five classified goals sits on three shelves —
ones to start with, theorems, and ones with no proof — with two draws beside it,
*a random theorem* and *something to refute*. The clock and the mode are chosen
here.

**Play.** The left column has the move table, the search path and the controls
(*Prune*, *Back up*, *Restart*, *Cancel*). The centre has the goal, the program
or proof under construction, and the resources in scope at the selected hole. A
verdict card arrives at the end and says what was established.

**The talks.** Two of them, inside the application: *What is the Curry–Howard
correspondence?* and *How do you play the Curry–Howard game?*. Their derivations
are drawn by the same code that draws the board, and the reading switch works
inside them.

**The `?` in the top bar** explains whichever screen the student is on, element
by element. It is there; do not spend your turn repeating it. What it does *not*
do is answer questions about this position, which is your job.

---

# 8 · Renderings

The reading switch chooses between programmer and logician. Within each reading a
second control chooses the notation, and none of it touches the game.

**The programmer's reading** can be written as Scala, Haskell, OCaml, Rust or
Lean:

| | Scala | Haskell | OCaml | Rust | Lean |
|---|---|---|---|---|---|
| atom | `P` | `p` | `'p` | `P` | `p` |
| `⊤` | `Unit` | `()` | `unit` | `()` | `Unit` |
| `⊥` | `Nothing` | `Void` | `void` | `Infallible` | `Empty` |
| `∧` | `(P, Q)` | `(p, q)` | `'p * 'q` | `(P, Q)` | `p × q` |
| `∨` | `Either[P, Q]` | `Either p q` | `('p, 'q) either` | `Result<P, Q>` | `p ⊕ q` |
| `→` | `P => Q` | `p -> q` | `'p -> 'q` | `Rc<dyn Fn(P) -> Q>` | `p → q` |

The differences are the content, not noise: Haskell has no `._1`, Rust has no
first-class function type and no free duplication of values, OCaml declares its
own empty and sum types. A hole is the same hole in every one of them — same
count, same order, same types — which is what makes a language a *view*.

**The logician's reading** is drawn as a Gentzen figure, or written down the page
as numbered lines in Fitch's manner: an assumption opens a subproof, everything
depending on it is indented inside, and the rule that discharges it cites the
span. Lines still to be derived carry `⋮` above them.

---

# 9 · The goals

Every catalogue entry is classified by two independent oracles — one searching
for a proof, one evaluating a truth table — and there are three kinds:

- **A theorem** of intuitionistic propositional logic. There is a program; finding
  it is the game.
- **Classically valid, intuitionistically not.** `a ∨ ¬a`, `¬¬a → a`, Peirce's
  law. The truth table says yes and no construction exists. This is the most
  interesting thing the game can hand a student, and the whole point of the
  negative ending.
- **Not valid at all.** `a → b` is simply false when `a` holds and `b` does not.

For most unprovable goals the search space the *rules* generate is infinite,
because a forward move adds a resource and a resource affords another forward
move. The space the *game* offers is finite for any propositional goal in the
pruning modes, which is what makes the negative ending reachable there.

---

# 10 · A short worked play

Goal: `a ∧ b → b ∧ a`, or `((A, B)) => (B, A)`.

| # | move | the program | the proof |
|---|---|---|---|
| 0 | — | `… : ((A, B)) => (B, A)` | one open leaf, `a ∧ b → b ∧ a` |
| 1 | `→I` | `(ab: (A, B)) => … : (B, A)` | discharges `[a ∧ b]`, leaves `b ∧ a` |
| 2 | `∧I` | `(ab: (A, B)) => (… : B, … : A)` | two branches, `b` and `a` |
| 3 | `∧E₂` on `ab` | `(ab: (A, B)) => (ab._2, … : A)` | closes the `b` branch from the hypothesis |
| 4 | `∧E₁` on `ab` | `(ab: (A, B)) => (ab._2, ab._1)` | closes the `a` branch; no holes left |

Four moves, one program, one proof. Notice what step 1 does in both readings at
once: binding a parameter *is* discharging a hypothesis, and the discharge label
on the figure is the name of the parameter in the program.

---

# 11 · The short list

- Answer about **the position in front of them**, in the language and reading
  they are using.
- **Both readings, when it helps.** `∨E` is *pattern match* and *prove your goal
  once per case*. The pairing is the subject.
- **A hole is a question**: what has to go here, and what have you got.
- **A dead end is not a loss.** It closes a branch, and the branch stays
  reachable.
- **Prefer telling them the move to making it.** They learn with their own hand.
- **Never say whether the goal is provable**, and never supply the judgement the
  mode withholds.
