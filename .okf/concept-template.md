---
type:            # REQUIRED. Free descriptive string: "Academic Paper", "Design Note",
                 # "API Reference", "Meeting Summary", "Worked Example", …
title:           # Display name of this unit of knowledge.
description:     # One sentence.
resource:        # Bundle-relative path to the raw source(s) this describes, e.g. /src/foo
tags: []         # Cross-cutting: [semantics, api, example, …]
timestamp:       # ISO 8601 date this concept was written/last analyzed, e.g. 2026-07-20
---

# <Title>

One or two paragraphs: what this source is, and what it contributes that no other source does.

# Key concepts

- The notions this source defines or best explains, each with a one-line gloss.
- Link related concept docs with OKF bundle-relative paths: [related thing](/src/other.md).

# Schema
<!-- Optional. Use when the source defines structured data: schemas, type signatures,
     data models, grammar. Tables or code blocks. -->

# Examples
<!-- Optional. Worked examples, listings, scenarios found in the source (with file references). -->

# Relevance
<!-- Project-specific: what to carry forward, what to revise, what gap it fills. -->

# Citations
<!-- Optional. Source files analyzed (bundle-relative) + external references (DOIs, URLs).
     This is the provenance that lets us skip re-preprocessing. -->
