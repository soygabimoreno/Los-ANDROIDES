---
name: github-issue-context
description: |
  Load the full context of a GitHub issue in Los ANDROIDES (body, comments, labels,
  assignees, linked PRs) before proposing an implementation, refactor, or fix that
  relates to it. Trigger on: a URL like `github.com/.../issues/N`, a mention of
  `issue #N` / `#N`, "this issue", or "what does the issue say".
---

# GitHub Issue Context — Los ANDROIDES

Use this skill the moment an issue is referenced. Without the issue body and the latest comments you risk solving the wrong problem.

## Workflow

1. Resolve the issue number. From a URL like `https://github.com/<owner>/<repo>/issues/123` take `123`. From `#123` use it directly.
2. Fetch in one call:
   ```bash
   gh issue view <n> --json number,title,state,body,labels,assignees,comments,url
   ```
3. Fetch linked PRs (if any reference the issue):
   ```bash
   gh pr list --search "linked:issue-<n>" --json number,title,state,url
   ```
4. Read the body and every comment. Pay special attention to the most recent comments — direction often shifts there.
5. Produce a compact context block (see format below) and use it as the foundation for whatever the user asks next.

## Output format

```
## Issue #<n> — <title>
State: <open|closed>  ·  Labels: <l1, l2>  ·  Assignees: <user>

## Summary of the ask
<2–4 sentences distilling the request and any constraints>

## Key decisions in the thread
- <date> <author>: <decision or pivot>
- ...

## Open questions
- <anything the thread leaves unresolved>

## Linked PRs
- #<m> — <title> (<state>)

## Suggested next step
<one sentence — what you would do next, given the above>
```

## Rules

- Never propose code based on the issue title alone. Read the body and the comments first.
- If the issue is closed, say so up front and ask the user whether they still want to act on it.
- Do not comment, close, label, or assign the issue from this skill. Use `/create-issue` or `gh` directly for write actions.
- If `gh` is not authenticated, stop and tell the user to run `gh auth login`.
