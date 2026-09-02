---
description: Stop the Curry–Howard game server started by the Claude Code plugin
---

Stop the game server and report the result in one line. Do not restart it and
do not kill a process by PID if the authenticated stop operation reports that
no server is running.

Run:

```sh
"${CLAUDE_PLUGIN_ROOT}/skills/curry-howard-game/scripts/serve.py" --out "${CLAUDE_PLUGIN_DATA}/runtime" --stop
```
