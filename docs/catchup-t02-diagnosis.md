# T02 inventory causal comparison

**NOT_READY. Do not integrate.** T02 changes documentation and a portable evidence packet only. The blocked executable remains `771713b97cb1713666c12b38900a588d9ea4f975` and includes exact master `bd191a007b63a8ef94baccfb41f606ffec823ef1`. The final SHA is recorded externally in `T02/raw/t02-acceptance.json`; the committed packet is [catchup-t02-acceptance.json](catchup-t02-acceptance.json).

## Earliest established service divergence

At helper/nursery **1/1**, the failed candidate's first forager is `03117619-ce7e-3ec1-895b-dd8c555a9a0f`, registry insertion index4, at relative `(4,-2,-1)`. Its actual controller RETURN is `OPENING / physical_adult_meal`, holding one apple, mealTicks1, sugar0, spentSugar4000 and fasting12002. Exact master's first sample instead selects `08d36b3b-cb80-396c-b038-b22e882622b7`, index42 at `(3,-2,-1)`: `EXIT / authorized_exit_open`, empty hand, mealTicks0, sugar0, spentSugar4000 and fasting12002.

The unchanged `QueenFounding.tick` comparator orders side-row displacement, then UUID text. Replaying that comparator over each preserved initial actor list selects the actual first forager in all four observations. This replay does not invoke a controller, consume randomness or re-evaluate eligibility. It explains the different selection for the captured input lists; it does not establish that this early difference causes the timeout.

All four start with49 workers, food8, stone32,44 paid tier-two clay cores, no new brood or work receipts; worker age28000, fasting12000, sugar0 and4000 historical spent sugar; queen nutrition4000/8000. Initial role-position vectors and maintenance history agree. Actual queen, worker and brood UUIDs differ. The insertion order is preserved in the native registry-based periodic snapshot and crosschecked against the retained on-disk `colony_members` order. Initial entity-query order is recorded separately, without normalizing away identity/order effects.

The three cohort runs use the exact nine names from the failed shard in one JVM. Assignment order alone does not fix vanilla registry filtering, environment batching, test scheduling or later entity iteration. The isolated candidate has one selected case. All four world-generation settings record seed0, but UUID.randomUUID is not controlled by that seed. Absolute origins are failed `(-5916076,6,11090338)`, master1 `(-12813416,6,4949892)`, master2 `(-14942416,6,-8412775)` and isolated `(-262041,6,-6629821)`. Cohort surface pads retain71-block relative spacing; the inventory pad is fourth. Original fixture direction is EAST and the annotation's rotation defaults to0; actual helper rotation was not separately logged. Live marker/queen offsets agree with that geometry. World UUIDs, manifest hashes and every initial actor UUID/order are in the portable packet. No matched initial world/RNG state is claimed.

## Construction: one route interval, not merely completion time

The first builder is assigned at helper101 in both failed candidate and master1. Candidate actor `0acef5bf-523f-3884-bfd5-8d6c96519786` (index38) has sugar0/fasting12102; master1 `18798f1d-c24a-366e-861e-d71c7406c0a1` (index2) has sugar994/fasting0. Their actual path/position operands differ. The candidate changes builder to `0969af6b-abb2-38f1-8005-532c72113bc3` (index3) for the material store, whereas master1 retains its first builder. These are actual identities, not interchangeable role labels.

All44 ordered withdrawals and44 physical conversions have matching relative wall targets and recipe stone. The completion assertions preserve44 payments,44 retained clay cores, zero released material, zero cargo and no remaining claims. The paired ledger localizes the largest interval difference to **material-store wall19**, overall conversion27, relative target `(6,-2,3)`:

| Native event | Failed candidate helper/loaded | Exact master1 helper/loaded |
|---|---:|---:|
| Previous wall18 conversion | 2945/2945 | 2957/2957 |
| Wall19 conversion | 4224/4224 | 3174/3174 |
| Between conversions | 1279 | 217 |

Candidate physically withdraws that stone at2964, reaches the unchanged `upgrade_route_stalled_cargo_retained` controller bound at4165, starts `physical_stone_facing_action` at4205 and converts at4224. From3400 through4000 the sampled position is exactly `(6.820384057238698,-2,0.3229430429637432)` relative to entrance; path nodes8, nextIndex3, canReach=true, done=false, target `(-5916039,8,11090373)`, onGround=true, sharingBusy=false, cargo one stone. Nutrition continues spending normally in those samples. This is an observed navigation stall and subsequent physical recovery, not a missing-material or released-claim explanation. The interval expands by1062 ticks; total construction completion differs by1201 (8654 versus7453). Neither number proves the whole counterfactual.

`T01` failed log SHA256 `83722db24d220e5ca117450e29e8691c278102bb3b0ee7a750f962e15be1c383`: actual wall conversion at line3755; helper2964 withdrawal/RETURN at line2994; stalled RETURN at line3726. Master1 corresponding conversion is line2998 in log SHA256 `2ac6e3498d8f189ca5b29a07509c03633af1a20e7701088c8eec4397cade66ef`. Full paired rows include both clocks, native line numbers, actors, equipment and target coordinates.

Eighteen source method bodies are identical to exact master, including QueenFounding tick/claimForager, ChamberUpgrade consider and WorkerTasks tick/eligibility/upgrade admission/fetch/build/stand/work-point/navigation/material/food search/pickup/deposit methods. This narrows the investigation; shared AdultLife, BroodPile, nutrition snapshots, membership indexing and deterministic newborn identities still require equivalent-input reasoning. Zero aggregate invocations is not proof that integration has no ordinary effects.

The original trace does not record each `wallStand` candidate/result, full path waypoint list, createPath/moveTo return, velocity/collision state, exact per-tick scheduling or RNG choice. It therefore cannot establish which original navigation operand first caused the stall, or a catch-up-only semantic correction. Pinning fortunate identities, altering task priority, changing stands/navigation or adding supplies would not be an authorized repair on this evidence.

## Later service and current stocks

| Observation | Construction complete | First clay pickup | First clay deposit | First actual material65 | Completion to material65 | Inventory endpoint |
|---|---:|---:|---:|---:|---:|---:|
| failed | 8654 | 11672 | 11865 | 19965 | 11311 | timeout 20,000 |
| master1 | 7453 | 7471 | 7666 | 17305 | 9852 | 17306 |
| master2 | 7453 | 7471 | 7664 | 18345 | 10892 | 19126 |
| isolated | 7453 | 7471 | 7666 | 19446 | 11993 | 19447 |

These deposit clocks are actual controller RETURN clocks. The test observes their new stocks on its following helper callback. The failed candidate makes **61 actual apple/chicken pickups** between construction completion and the first clay deposit; each passing observation makes0 in its corresponding short interval. Candidate first clay pickup is3018 ticks after completion, first clay deposit3211 ticks after completion. The ordinary food-before-material policy and actual source/cargo/stock records are retained, not changed. This establishes a later service-path difference, without proving why the earlier trajectories produced those food operands.

Material65 occurs at19965 in the failed run, only34 ticks before the last in-horizon19999 observation. The unchanged test branch therefore triggers the extra wave on its next callback (19966, inferred from source/event ordering). The native19999 observation directly proves both actual drops remain on the ground:18 apples,18 chickens, with no successful food delivery after material65; cache is still5 apples+5 chickens. The latest food delivery was19325. The separate post-timeout endpoint20002 is not acceptance time.

Master1 already has food14 when material65 is observed at17306; the isolated candidate already has food13 at19447. Master2 waits from material65 at18345 until a physical food13 delivery at19125, then passes at19126. Even the isolated passing candidate's completion-to-material65 span11993 exceeds the failed candidate's11311. Consequently construction timing alone does not explain the simultaneous-stock failure, and another unchanged pass cannot qualify a correction or exception.

## Verdict and unresolved work

No catch-up integration defect was demonstrated. No speculative gameplay edit or diagnostic overlay was added, and no new GameTest JVM was launched. T01's exact151 affected identities remain150 pass/one unexempted timeout; the interrupted enclosing wrapper remains unknown and its seven-case child remains separately verified. No exact-master exception qualifies. Final unfiltered fast/complete/assembly are unlaunched because affected acceptance remains blocked.

The precise remaining step is an equivalent-input demonstration of the first differing integration operand at the material-store wall19 route, with original identity/order and complete navigation decisions captured passively, followed by a causal link to the late food/material service and the original simultaneous-stock predicate before20000. This packet does not assert that a catch-up correction exists. An ordinary navigation/priority/food-policy change would need a separate owner decision after demonstrating why a catch-up-only correction is insufficient; no such decision is currently requested or inferred.

Growth remains300 steps/25% of a stage (measured maxima264/97), caregiver interval92, all original exact surface-access assertions,210 per-kind conservation equations, two outcome cargo controls, six scarcity cases and nectar0 v1/1 v2. No historical690 serialization-order or zero-debt-opening diagnosis is reopened. The22-event observation did not demonstrate continuation across an actual512-event cutoff, and scarcity larvae did not emerge within preserved horizons.
