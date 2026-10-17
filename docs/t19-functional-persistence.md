# Functional persistence checkpoint, 2026-10-17 (stage1c T19)

The opt-in development harness has demonstrated a nonempty normal save across two
fresh server-process reopens. The successful functional world starts with a genuinely generated
Young colony (queen +4 workers) and Mature colony (queen +24 workers). Generated
chambers, food and sixteen initial Mature clay units are initial state. Naturally
laid brood and later physical collection, feeding, hauling and paid wall prefixes
are separately observed production actions. No adult growth, completed construction
or ingestion is attributed to the supplied initial state.

An independent generated Young control first passed at loaded tick **6,401**:
normal production laid brood `4cde5227-1b92-3168-a63d-d14a69804914`, followed by
200 consecutive loaded development steps. Its rolling income was **8,000 sugar**
from two real apple ingestions, above the 4,500 threshold for five adults plus the
prospective sixth. Generated queen sugar is stock, not a receipt. Current funding,
protein, commitments and per-kind physical food equations were retained. Work/brood
multipliers were 1/1, ordinary target 20 TPS, lifespan 144,000 and fasting grace
24,000, with normal AI and item expiry.

The first two-colony preparation is a retained negative at **26,000 loaded ticks**.
Both material stores were empty, no qualifying live material carrier remained,
and clay lay uncollected beside continuing food work. Its two clay deposits and
eighteen real wall payments did not satisfy the later close boundary. No reopen
was launched. The corrected prospective recipe removes six later competing food
waves: one finite wave at loaded 5,600 supplies eight apples, four chickens and
eight clay per colony. All future sources enter during A; B/C add nothing. Its
12,000-tick preparation bound is shorter than the failed bound.

| Process | Normal save game time | Observed result |
|---|---:|---|
| A, fresh preparation | 7,575 | Qualifies at loaded 7,560 with identified progressing brood, food/material inventories, and distinct current food/material carriers with valid claims/tasks and destination. |
| B, first reopen | 8,240 | Exact saved/first-decoded comparison; real queen feeding and clay deposit; 214 subsequent ordinary ticks; another qualified nonempty boundary for C. |
| C, second reopen | 14,253 | Exact second comparison; real adult apple meal and clay deposit/payment; 200 subsequent ordinary ticks with continuing identified Young brood development. |

All three native/Gradle/phase-wrapper exits are zero. Native PID plus process start
identity differs per phase. A single lease protects the same resolved world; every
post-close file hash is checked before the next launch. Executable inputs remain
frozen through the chain. Independent native-disk accounting balances all 24 apples,
12 chickens and 32 clay, including ordinary expiry, ingestion and paid construction.

[PersistenceHarness](../src/gametest/java/dev/primeants/gametest/PersistenceHarness.java)
is selected only by an explicit T19 declaration. The historical restart and scale
users retain their behavior. [run-persistence.py](../scripts/run-persistence.py)
creates a fresh owned world and runs A/B/C sequentially; it rejects diagnostic-only
declarations. [persistence-disk.py](../scripts/persistence-disk.py) reads actual native
NBT/Anvil after process exit. The loading observations retain each first value once,
all ordered inventory/components, raw block/SavedData envelopes and decoded fields.
The source audit finds 21 SavedData types, including four absent from the legacy
seventeen-table observer. Passive entity capture reads the actual loaded founding
reason field without making an enclosure query or creating canonical authority.

The boundaries remain distinct. Native final saving recomputes the readiness display
while chunks unload; both preclose and actual saved strings are retained, with the
independently demonstrated reason for that difference. Actual disk versus first
decoded state is exact, including that saved string; there is no field exception,
growth tolerance or cursor/receipt/claim normalization. Subsequent ordinary readiness
changes are separate. Automatic catch-up remains enabled: actual startup intervals
are Young/Mature **2/4 ticks in B**, **2/3 in C**. Full observation/catch-up clocks and
independent food/material accounts are retained. A completed catch-up keeps its
`Through` endpoint; `Cursor=-1` denotes no pending work.

Acceptance is scoped to this finite functional world and graceful shutdown. All
30 adults remain canonical, with four then five nonfounding brood records; no adult
emergence or death is claimed. Terminal histories, custody, mining, native food and
tier-three state are empty or unexercised. The exact physical projection contains
43,560 cells covering the declared colony work, markers and dropped sources; virtual
outside-height observations are separate. The outer native-food search shell is
unexercised and not projected. All native world-file hashes remain available. This
does not establish abrupt-crash durability, migration, oversized physical inventories,
Great-colony persistence or sustainability.

External local evidence is under
`<turnloop>/directions/prime-ants-stage1c/turns/T19/`: the frozen original and amended
protocols, preserved negatives, complete first comparisons, named service events,
consecutive development rows, process/exits, world hashes and regression reconciliation
are linked from `raw/t19-persistence-acceptance.json`. The raw gate's `settled` field
is captured before the current tick increments the counter; independent game-time
endpoints and 201 consecutive states prove the 200 post-service development steps.

Fresh fast acceptance passes **77 server /124 unit /26 model** cases. The required
selected long run is **19/20**, with native exits `[0,0,1]` and Gradle/root exit1.
`tier_three_game_test_physical_oversized_inventories_survive_in_memory_reconstruction_and_capacity_loss`
times out at its unchanged 20,000-tick bound. Its last first-unmet predicate is
`foodAbove12`: all three tier-three jobs are complete (8/22/14 paid cells), material
stock is 69/96, but food is five chickens/18. The fixture's required simultaneous
inventory gate remains unmet. This timeout is nonexempt. Native and merged failure
elements, the T18 complete-context pass and unchanged fixture hashes are retained;
cause is unresolved, with no correction, waiver, changed bound or unchanged rerun.
The two-reopen functional proof is positive; **overall T19 acceptance remains partial**
and release correctness is blocked by this fresh regression.

Production and ordinary serialization remain unchanged. T17's withdrawn lookup
candidate remains absent; its lifecycle assertion history remains unresolved and
nonexempt. T18 accepted current correctness without a causal repair. Performance
limitations stay documented and outside this persistence gate. Version 0.2.0,
clean-client acceptance, Survival screenshots, final EN/RU notes and GitHub publication
remain release work.
