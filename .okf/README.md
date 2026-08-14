# .okf — Open Knowledge Format tooling

Machinery for the repository-wide OKF knowledge bundle (root: [`/index.md`](../index.md)). This `.okf/` directory is a dot-directory and therefore sits **outside** the knowledge layer — its `*.md` files are tooling docs, not OKF concept documents.

> The agent-facing workflow (how to analyze sources into concept docs) is defined in [`../AGENTS.md`](../AGENTS.md). This file documents the tooling that supports it.

- [`hooks/pre-commit`](hooks/pre-commit) — appends newly-added files under the watched area to a `log.md` at commit time, grouped by date, and stages the log into the same commit. Ignores dot-directories, the OKF meta files, and editor/VCS cruft. Set `WATCH` and `LOG` at the top of the hook.
- [`install.sh`](install.sh) — symlinks the hook into `.git/hooks/`. Run once per clone: `sh .okf/install.sh`. Safe to re-run.
- [`concept-template.md`](concept-template.md) — the shape every OKF concept document follows.

## The bundle

The **repository root** is the bundle root; every `*.md` outside dot-directories is an OKF document with a `type`. Reserved files per directory: `index.md` (navigable table of contents) and `log.md` (chronological provenance). Everything else `*.md` is a concept document.

## Incremental conversion workflow

Concept documents are written **on demand**, the first time a source is analyzed — never in a bulk preprocessing pass. When analyzing a source:

1. Check for an existing concept doc next to it (mirrored path, e.g. `src/foo.md` describes `src/foo/`). **If it exists and covers what you need, read it instead of re-reading the raw source.** That is the whole point — analyze once.
2. If not, analyze the source and write a concept doc from [`concept-template.md`](concept-template.md), placed as a sibling of the source it describes.
3. Add a one-line pointer under the `<!-- okf:concepts -->` marker in the relevant `index.md`.

Consumers must be permissive (per the OKF spec): missing concept docs, unknown `type` values, and broken links are all expected while the bundle is partial.
