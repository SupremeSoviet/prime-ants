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

## Completed merged acceptance attempt (2026-10-16)

Merge commit `93ee170f6709f72f8ae089fc6064b9a56c4f290c` retains both authorized
parents. The tested GameTest implementation is committed as
`345baabd2ae0dadace86c79963abd78e8e225d13`; production and client sources have
no changes after the authorized merge. Fresh discovery confirms the exact parent
union: 386 server identities (77 fast, 309 long), 124 unit and 26 model tests,
including all 38 historical catchup registrations.

The final identity comparison uses one four-case fresh server and four isolated
fresh servers. Initial colony identities, relevant comparator inputs and ordering,
common actual claim prefixes and 91 common successful-birth ordinals match.
Derived worker identities match and the actual observations have no cross-case
identity collisions. Raw tier-three entity traversal differs; its total comparator
and relevant selection results match. This establishes the declared identity and
selection milestone, not identical whole-server physics. The four-case cohort
passed 4/4; isolated Mature and surface cases retained their original 60,000-tick
timeouts. Those failures remain red.

The required fresh, unfiltered commands were executed with three shards and
concurrency three, `--console=plain --rerun-tasks`:

- `checkFast`: exit 0, 142.425 seconds, 77/77 server, 124/124 unit, 26/26 model.
- `checkLong`: exit 1, 6770.948 seconds, 308/309 server; native exits `[0, 1, 0]`.

Native and merged XML match discovery exactly once for all 386 identities. The
sole complete-run failure is the supplied founding-to-Mature surface case at the
unchanged 60,000-tick deadline. At tick 60,000 its cached stage inputs were Young,
23/25 adults, food 6, clay 16, with evaluation age 7 loaded ticks; the snapshot
contained 22 living canonical workers plus the queen. No brood remained, the last
lay was nursery-loaded tick 14,741, and the recorded growth reason was
`recent_income_below_adult_commitments`. These are observed preparation limits;
the first causal physical divergence remains UNKNOWN. No physical preparation,
history, budget, geometry, assertion or deadline was changed to rescue the run.

The complete run retains F2's actual simultaneous two claimed clay carriers and
eight-clay accounting (pass tick 21,742, original bound 30,000). The Mature ledger
case records actual builder death at 33,883 and exact subsequent accounting at
35,044. Tier-three reconstruction/loss passes at 19,266: every one of its five
boundaries contains current food 14 and materials 66, with capacities 18/96 before
loss and 6/32 after loss. Exact ordered contents and serialized-field assertions
remain intact. A separate one-server 4/4 target records two actual cargo-reload /
missing-supply reconstruction overlaps, with nonempty claimed-actor witnesses;
separate-server passes are not substituted for that evidence.

The complete merged green checkpoint and T11 optimization acceptance are withheld.
One complete merged attempt was run; no unchanged broad retry was selected.
Historical no-new-green=3 is preserved. Pre-milestone variable long timeouts were
exempt until the recorded identity milestone; the final non-green checkpoint adds
one, yielding cumulative no-new-green=4. Isolated passes did not reset it. A further
physical founder-growth repair needs a separately bounded owner decision within
the preserved obligations; T14's identity work does not authorize changing biology
or food supply.

Reviewable plans, comparisons, failure sequences, native/merged XML, unit/model
reports, executable archives, dependency hashes and actual exits are indexed by
`<turnloop>/directions/prime-ants-stage1c/turns/T14/raw/t14-merged-acceptance.json`.
Earlier NOT_READY evidence and unexplained T11/T13 failures remain separate.
Performance, Great/cap120, default-speed preparation, required cargo preparation,
two actual process reopens and release 0.2.0 remain outstanding.
