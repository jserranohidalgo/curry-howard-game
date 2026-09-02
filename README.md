# The Curry–Howard Game

Build a proof and write a program at the same time. They are the same thing, and
this game is about seeing that.

**▶ Play it: https://jserranohidalgo.github.io/curry-howard-game/**

Two talks come with it, and they are the place to start. Both run in the
application itself, with the real board and the real switch:

- **[What is the Curry–Howard correspondence?](https://jserranohidalgo.github.io/curry-howard-game/#correspondence)**
  — who Curry and Howard were, what they noticed, and one worked example carried
  from beginning to end.
- **[How do you play the Curry–Howard game?](https://jserranohidalgo.github.io/curry-howard-game/#guide)**
  — the board, the moves, the levels of help, and a full game won and another
  refuted.

No account, no server, nothing sent anywhere. Your game is saved in your own
browser, so you can close the tab and come back to it. You can install the page
as an app and play with no network at all.

## Play with a tutor

Alternatively, there is a Claude Code plugin that watches your game and answers
questions about the position in front of you: what the goal is asking, what you
have in scope, which rules apply here, why a branch closed. In Claude Code:

```
/plugin marketplace add jserranohidalgo/curry-howard-game
/plugin install curry-howard-game
```

Then ask it to play. It opens the game in your own browser and follows along. It
needs Python 3.

## Where it is used

| Course                                                                | University                 |
| --------------------------------------------------------------------- | -------------------------- |
| [Declarative Programming](https://github.com/jserranohidalgo/urjc-pd) | Rey Juan Carlos University |

## What is coming

**The source code and the design documents.** The game is written in Scala 3 and
Scala.js. The code and the documents behind it are being reviewed and will be
published here.

**More games.** The correspondence is a whole family: every logic has a language
whose programs are its proofs. This game is the first of them, and the others are
listed on the home screen so the shape is visible from the start.

|       | Logic                              | Language                                        |
| ----- | ---------------------------------- | ----------------------------------------------- |
| `λ→`  | intuitionistic propositional logic | simply typed lambda calculus **— playable now** |
| `λμ`  | classical propositional logic      | λμ-calculus, with first-class continuations     |
| `λΠ`  | first-order intuitionistic logic   | lambda calculus with dependent types            |
| `λμΠ` | first-order classical logic        | λμ-calculus with dependent types                |
| `λ⊸`  | multiplicative linear logic        | linear lambda calculus                          |

## Feedback

Bugs, ideas and things that were confusing are all welcome — open an issue here,
or write to
[juanmanuel.serrano@urjc.es](mailto:juanmanuel.serrano@urjc.es?subject=%5BThe%20Curry-Howard%20Game%5D%20),
which is the address in the game's *About* panel. If you report a problem from
inside a game, the tutor can attach the game itself: the goal and every move, so
the position can be replayed exactly.

## Releases

What changed, and when, is in [CHANGELOG.md](CHANGELOG.md). The version numbers
count the games: `0.1.x` is this one, and `1.0.0` waits until at least two of
them are here and stable.

## License

The code is MIT — see [LICENSE](LICENSE). The libraries it is built with have
their own licences, listed in [THIRD-PARTY.md](THIRD-PARTY.md). The photographs
and logos it shows are credited in
[docs/assets/credits.md](docs/assets/credits.md).
