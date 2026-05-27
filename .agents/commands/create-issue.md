---
description: Open a GitHub issue with a minimal template (context + acceptance criteria) in English. Offers labels from the repo if any exist.
argument-hint: <title>
---

You are creating a GitHub issue for Los ANDROIDES. Title provided: `$ARGUMENTS`.

If `$ARGUMENTS` is empty, ask for a title and stop.

## Steps

1. Run in parallel:
   - `gh auth status`
   - `gh label list --limit 50` (to know available labels)
   - `gh repo view --json nameWithOwner`
2. Abort if `gh` is not authenticated.
3. Draft the body in **English**:
   ```
   ## Context
   <1–3 sentences: what is the problem, where it shows up, what triggered the report>

   ## Acceptance criteria
   - [ ] <verifiable outcome 1>
   - [ ] <verifiable outcome 2>

   ## Notes
   <optional: links, screenshots, related issues, hypotheses>
   ```
4. If the repo has labels, propose 1–3 that match the issue (bug / enhancement / tech-debt / docs / etc.) and ask the user to confirm or override. Skip this step if the repo has no labels.
5. Show the draft (title + body + labels) and ask to confirm.
6. On confirm, create with:
   ```bash
   gh issue create --title "<title>" --body "$(cat <<'EOF' ... EOF)" [--label <l1> --label <l2>]
   ```
7. Print the resulting issue URL.

## Rules

- Title in English, ≤ 70 chars, no trailing period.
- Body in English.
- Do not assign anyone unless the user asks.
- Do not invent labels — only use ones already in the repo.
