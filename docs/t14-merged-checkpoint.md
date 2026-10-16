# T14 integration and deterministic GameTests (2026-10-16)

The owner's T14 instruction explicitly authorizes `git merge --no-ff catchup`
from clean master `de2c2024379940c16121ae7ab7bd0d011cd05c49`, integrating
catchup `738bd87eed010c42b157b2db4fe2565d7dc53243` (which contains `bd191a0`).
It supersedes the NOT_READY / do not integrate prohibition and conditional READY
prerequisite in catchup-merge-note.md, catchup-report.md and catchup-t02-diagnosis.md,
and the earlier direction's merge prohibition. Those documents and their negative
evidence remain intact. Integration does not retroactively turn their results green.

Git's ort merge completed without textual conflicts, retaining both parents.
Master's T11 canonical ownership recheck, T13 miner preparation and F2 observers
remain; catchup's reload dispatch, compatibility controls, scoped 300-step growth
tolerance, outcome cargo control and registrations are integrated. Fresh parent
discovery and exact registration-union checks are recorded externally in
`<turnloop>/directions/prime-ants-stage1c/turns/T14/raw/t14-registration-union.json`.

The next checkpoint requires test-only identity control and fresh fast and complete
long acceptance. Historical no-new-green=3 is retained; T14 is newly authorized.
Performance, process reopens and release remain outside this checkpoint.

## Test identity rule

The development source set uses namespace `prime_ants:gametest:identity:v1` and
UTF-8 name UUIDs over permanent case ID, per-case replica, creation path and a
path-local ordinal. Other cases, shard, process, directory and wall time do not
enter the key. Entity constructors retain their original RNG consumption; only
fresh test-scoped UUIDs change. Loads retain serialized UUIDs. Counters survive
in-process reconstruction, preventing fresh identity reuse. Concurrent cases have
disjoint namespaces; repeated colonies within a case receive distinct ordinals.

The ordinary founding clutch has a per-queen sequence. The first implementation
retained production's `prime_ants:egg:queen:loadedTick:slot` laying identity. Both
fresh four-case executions passed their gameplay assertions, but real natural-birth
sequences differed in F2, Mature and surface. That implementation is rejected for
determinism; its evidence is retained in external `raw/t14-identity-comparison-v1.json`.

Ordinary physical tests now use a per-queen successful-birth ordinal under the same
namespace. Failed predictions do not advance it. A committed tick/slot replay map
and the ordinal survive in-server nursery/entity reconstruction; loaded identities
are retained. The dedicated adult/event-clock, catchup and reconciliation control
classes retain the production event identity, including captured and deliberate
collision controls. Workers and custody retain their ordinary parent-derived IDs.
The effective prospective rule is external `raw/t14-determinism-plan-v2.json`.
Original captured/serialized and generated seed/dimension/chunk authority IDs are
retained. Test-only store release defaults are scoped; saved defaults still win.

Identity context is thread-local. Explicit UUID assignment/load binds context
without replacing loaded UUIDs; snapshot queries never grant another case ownership.
The shared fixture worker view sorts by UUID text. GameTest due callbacks use
registration order instead of object identity hashes. Neither change applies to
production selection or ticking. Passive query-return observations retain actual
production entity ordering, actual min returns, claim changes and side-row/UUID
comparator operands. The min observer preserves the original comparator and encounter
order and invokes it once. Vanilla RNG,
item velocity, navigation, chunk availability and server scheduling remain outside
the identity contract; stable identities do not prove whole-world replay.

The first linked-map scheduler exposed an actual nested-callback loss in the
unchanged 80-tick food lifecycle smoke. The old red and exact-jar reproducer are
retained. Structural new-key registrations during iteration now flush in order at
tick end; existing-key deadline updates stay immediate. No supplied callback time
or test bound changes. This also makes newly registered zero-delay callbacks
available on the next tick. No literal zero-delay schedules were found in the
authored fixtures. See external `raw/t14-callback-correction-declaration.json`.

The prospective rule, residual entropy, four-case comparison criteria and exact
commands are frozen before launch in external `raw/t14-determinism-plan.json`.

A subsequent read-only audit found one additional unordered test choice: the
representative mound cell used for chunk read-back. It now chooses the minimum
X/Y/Z cell. This changes no world cell; the existing checks still compare every
mound cell, every planned cell and the player block. The prospective declaration
is external `raw/t14-mound-order-declaration.json`. The four rotating fixtures,
their creation/selection paths and the namespace remain unchanged.
