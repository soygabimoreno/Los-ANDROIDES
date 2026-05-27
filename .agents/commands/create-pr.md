---
description: Push the current branch and open a GitHub PR with a Summary + Test plan body. Detects linked issues from branch name or commits.
argument-hint: [optional title override]
---

You are creating a pull request for Los ANDROIDES. Optional title hint from the user: `$ARGUMENTS`.

## Steps

1. Run in parallel:
   - `git status --short`
   - `git rev-parse --abbrev-ref HEAD` (current branch)
   - `git rev-parse --abbrev-ref --symbolic-full-name @{u} 2>/dev/null || echo NO_UPSTREAM`
   - `git log --oneline origin/main..HEAD 2>/dev/null || git log --oneline -10`
   - `gh auth status`
2. Abort with a clear message if:
   - There are uncommitted changes (`git status` non-empty).
   - Current branch is `main`.
   - `gh` is not authenticated.
3. If the branch has no upstream, push with `git push -u origin <branch>`. Otherwise `git push`.
4. **Title** (≤ 70 chars, English):
   - If `$ARGUMENTS` is given, use it.
   - Otherwise infer from the commits ahead of `main`. Imperative, present tense, no trailing period.
5. **Linked issue detection.** Look at:
   - Branch name (`feat/123-foo`, `fix/456-bar` → `Closes #123` / `Closes #456`).
   - Commit subjects (`refs #N`, `closes #N`).
   - If found, include the keyword in the body so GitHub auto-closes it on merge.
6. **Body** in English, two sections:
   ```
   ## Summary
   - <1–3 bullets, what changed and why>

   ## Test plan
   - [ ] <how to verify, manual or automated>
   ```
7. Create with `gh pr create --title "..." --body "$(cat <<'EOF' ... EOF)"`. Pass the body via heredoc to preserve formatting.
8. Print the resulting PR URL.

## Rules

- Title in English, ≤ 70 chars.
- Body in English.
- Never force-push.
- Never open a PR from `main`.
- Do not add any "Generated with Claude" footer unless the user asks.
