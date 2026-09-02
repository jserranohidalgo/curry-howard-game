---
description: Report a bug, a suggestion or something confusing about the Curry–Howard game
---

Write up the player's report on the game, following **Bugs, and what the game is
missing** in the skill's own instructions — that section is the procedure; this
command is only a way to ask for it by name.

In short: their words rather than a paraphrase, nothing added that you did not
see or hear, and **not mid-game** unless they have stopped — the clock does not
pause for a bug report.

Run, with the prose on stdin and one line of subject:

```sh
"${CLAUDE_PLUGIN_ROOT}/skills/curry-howard-game/scripts/serve.py" \
    --out "${CLAUDE_PLUGIN_DATA}/runtime" --report --subject "<one line>"
```

It writes the report with the current game attached — the play replays exactly,
which is the reproduction — and prints the file's path and a `mailto:` draft
whose subject already begins `[The Curry-Howard Game]`.

Show the draft in full before anything goes anywhere; it carries their game and
their words. Nothing is sent by you. When they agree, they can click the link,
or add `--open-mail` to open their mail client on the draft. A `mailto:` cannot
carry an attachment, so tell them to attach the report file the body names, plus
anything else they want to send.
