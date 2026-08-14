#!/bin/sh
# Install OKF git hooks into this clone's .git/hooks.
# Safe to re-run. Run from the repo root: sh .okf/install.sh
set -eu

ROOT=$(git rev-parse --show-toplevel)
cd "$ROOT"

SRC=".okf/hooks/pre-commit"
DEST=".git/hooks/pre-commit"

[ -f "$SRC" ] || { echo "install: $SRC not found" >&2; exit 1; }
chmod +x "$SRC"

if [ -e "$DEST" ] && [ ! -L "$DEST" ]; then
  echo "install: $DEST already exists and is not a symlink — leaving it alone." >&2
  echo "         Merge $SRC into it manually if you need both." >&2
  exit 1
fi

ln -sf "../../$SRC" "$DEST"
echo "installed: $DEST -> ../../$SRC"
