---
description: Stage-aware commit. Inspects staged changes (or offers to stage), infers the repo's commit style from recent log, writes the message in English, and asks before committing.
argument-hint: [optional scope or hint]
---

You are creating a git commit in Los ANDROIDES. The user invoked `/smart-commit` with this optional hint: `$ARGUMENTS`.

## Steps

1. Run these in parallel:
   - `git status --short`
   - `git diff --staged`
   - `git log --oneline -20`
2. If nothing is staged but there are unstaged changes, list them and ask which to stage. Do not run `git add .` or `git add -A` blindly.
3. Never stage files that look like secrets (`.env`, `*credentials*.json`, `*.pickle`, `google-services.json` if not already tracked, keystore files).
4. Infer the commit style from the recent log (conventional commits? prefixes like `feat:` / `fix:`? capitalization? imperative mood?). Match it.
5. Write the message in **English** (repo rule). Subject line ≤ 72 chars, imperative present tense. Add a body only if the change needs explanation beyond the subject.
6. Show the proposed message and ask the user to confirm before committing.
7. On confirm: commit normally. Never use `--no-verify` unless the user explicitly asks. If a hook fails, fix the underlying issue and create a new commit (do not `--amend` to bypass).

## Rules

- Do not commit if `git status` shows merge conflicts or a rebase in progress.
- Do not push.
- Do not amend a previously-pushed commit.
- If the staged diff spans multiple unrelated concerns, suggest splitting and ask.
