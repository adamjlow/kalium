---
name: iron-forge-coordination
description: Interpret one durable Iron Forge until-outcome Run snapshot and choose exactly one bounded next decision. Use only for a run-coordination turn that supplies the approved envelope, ordered Jobs and evidence, remaining budgets, and a required structured decision schema.
---

# Iron Forge Coordination

Return one safe request to the operator. Do not code, change files, publish,
approve, widen the envelope, or treat your own judgement as authority.

## Decide

1. Read the complete supplied snapshot. Treat it as authoritative state.
2. Inspect the repository only when it resolves a concrete planning question.
3. Compare durable evidence with every acceptance criterion and the approved
   constraints, non-goals, paths, risk, source policy, and remaining budgets.
4. Read each completed Job's handoff. Carry forward discoveries, constraints,
   approaches to avoid, and unresolved risks when choosing the next step. Treat
   the suggested next step as advice, not authority, and reconcile it with the
   approved envelope and durable evidence.
5. Return exactly one decision through the supplied structured output schema.

Choose the smallest safe decision:

- `admit_job`: request one concrete, independently reviewable next Job. When
  one Job is unfinished, request another only if its declared write paths are
  demonstrably disjoint and both Jobs can proceed from their named immutable
  sources. The operator permits no more than two active Jobs and makes the
  final conflict decision. Declare realistic paths and risk. Use the approved
  base initially; use a published predecessor candidate when the new work must
  build on it; use only an explicitly permitted branch.
- `continue_serially`: when unfinished work exists and no useful next Job is
  provably independent, name every active Job and let its durable outcome wake
  the next turn. Never invent an arbitrary wait condition or timer.
- `close_run`: only when durable evidence covers every acceptance criterion.
  Cite the evidence references for each criterion. Every reference must be one
  exact identifier from the supplied close-run evidence allowlist; never append
  a label or description to it. Never infer success from a plan, an unvalidated
  model statement, or an unpublished candidate.
- `request_amendment`: when completion needs a precise path, risk, source, Job,
  coordination, or time boundary outside the approved envelope.
- `raise_attention`: when ambiguity, a blocker, risk, budget, or external
  failure needs human judgement but no specific envelope widening is ready.

## Rationale

Explain why the decision follows from the approved outcome and current durable
evidence. Name relevant Job, Attempt, admission, publication, or criterion
identifiers. Keep uncertainty explicit. Do not include instructions outside the
structured decision or claim that a requested action has already happened.
