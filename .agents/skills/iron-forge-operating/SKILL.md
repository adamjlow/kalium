---
name: iron-forge-operating
description: Inspect and operate this repository through Iron Forge's native MCP tools. Use for status, change proposals, approvals, Runs, and cancellation.
---

# Iron Forge operating procedure

Repository files are the source of truth for product and implementation
content. Iron Forge is the source of truth for durable Proposal, Approval, Run,
Job, Attempt, and Evidence state.

## Inspect before acting

1. Call `get_home` for the configured project and concise durable counts.
2. Call `get_proposal` or `get_run` when the user asks about specific work.
3. Report the returned freshness and state. Never infer execution from a branch,
   file, or chat message.

Status questions are read-only. Do not create a Proposal merely because the
user asks what is happening.

## Propose a change

Call `propose_change` only after an explicit request to change the repository.
First call `get_home`; use its project repository and controlled
`source.revision` as the full immutable base commit. The user should not need
to supply repository coordinates or a Git SHA in ordinary conversation.
Supply user-level intent:

- outcome;
- repository and full immutable base commit;
- allowed and forbidden paths;
- acceptance criteria;
- risk;
- optional constraints and non-goals;
- a stable idempotency key reused for an identical retry.

Do not invent a role, Job graph, runner, engine topology, environment, skill
version, validation command, or publication mechanism. Iron Forge derives
those from controlled project configuration.

`propose_change` records an immutable Proposal and pending Approval. It does
not create or start a Run. The user must approve the exact revision through the
separate human confirmation boundary. Never ask for, capture, or simulate the
human secret or one-time proof.

After approval, call `get_proposal` again to obtain its durable `runId`, then
use `get_run` for authoritative status.

## Runs and cancellation

An approved Proposal creates exactly one Run and one queued Job. Use `get_run`
for authoritative lifecycle and evidence. Call `cancel_run` only after an
explicit cancellation request, using the version returned by the latest read.
On a version conflict, reread before taking another action.

Never claim execution, validation, a commit, or publication unless the
corresponding Attempt and Evidence records say it occurred.

Native Claude or Codex messages are not durable Iron Forge conversation state.
Only MCP actions and their resulting PostgreSQL state are authoritative.
