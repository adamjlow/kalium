---
name: iron-forge-execution
description: Execute one approved, bounded Iron Forge repository change inside an isolated runner workspace.
---

# Iron Forge execution procedure

Use this procedure only when an Iron Forge runner supplies an approved Proposal
and isolated workspace.

1. Read the repository's `AGENTS.md`, `CLAUDE.md`, approved outcome, scope,
   acceptance criteria, constraints, and non-goals.
2. Inspect the relevant source before changing it. Keep the implementation
   inside the approved allowed paths and outside forbidden paths.
3. Use the native agent runtime for exploration, edits, and context management.
   Do not invent another workflow, role hierarchy, or execution topology.
4. Run focused checks while iterating. The runner performs the canonical
   host-selected validation separately; never claim that validation passed
   merely because a model or ad hoc command says so.
5. Return a concise account of changed paths, checks actually observed,
   remaining risks, and any reason the approved outcome could not be completed.

Never access operator, database, confirmation, Docker-control, or publication
credentials. Never publish branches or pull requests directly.
