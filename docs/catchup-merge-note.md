# Catch-up merge note

**NOT_READY. Do not integrate.**

Current catchup executable: `771713b97cb1713666c12b38900a588d9ea4f975`. Merged current master: `bd191a007b63a8ef94baccfb41f606ffec823ef1`. The final documentation-only catchup commit is the packet's `final_commit`. Integration is prohibited. The external catchup6/T01 `raw/t01-acceptance.json` and `report.md` contain the exact evidence paths and final SHA.

All **151 distinct affected identities were executed**: A 40, verified completed old B child 7, fresh remainder 27, and unfiltered affected fast 77. **150 pass; one unexempted failure remains.** All historical 38, all six historical failures, 15 added master controls and two catch-up regressions are present. Five historical failure identities pass; inventory fails. No missing, duplicate, unexpected, stale, errored or skipped identity is accepted as a pass.

The precise blocker is `prime_ants_test:tier_three_game_test_physical_oversized_inventories_survive_in_memory_reconstruction_and_capacity_loss` in `affected18-b-remainder-resumed`. The enclosing native/supervised exits are **1/1**, duration **2356.732 seconds**, with 26/27 cases passing. Native timeout remains 20,000 ticks. At helper 19,999, the first unmet predicate is `foodAbove12`: food 10, materials 65; four tier-three functions and 44 paid cells are complete. Zero aggregate invocations are recorded. Construction completed at 8,654; at 19,500 the material gate and extra food wave were still false. These operands establish the failed simultaneous stock condition, not its first cause.

Exact current master passed the same nine-case cohort twice; the candidate passed isolated at 19,447 with native/supervisor 0/0 in 1,454.782 seconds. The first master sample passed at 17,306 with 0/0 in 1,284.587 seconds. The second sample's actual endpoint, duration, reports and unchanged 1,019-blob source export are in `master-baseline-bd/evidence/` and the final packet. **No master exception is qualified, and no causal correction is demonstrated.** A pass in another context does not erase the adverse cohort result. Zero aggregate invocations alone do not exclude every integration effect in shared ordinary code.

Final unfiltered `fast18-final` and `complete18` were **not launched**: affected acceptance is not green and no named master exception is demonstrated. Final check/assemble graph, fresh complete 386-case acceptance and final assembly are therefore **not certified**. Existing build outputs and older source archives are not substituted for them. Current discovery/preflight derives **386 server identities (77 fast+309 long), 124 unit, 26 model**; this is discovery, not a final execution claim.

Read [the report](catchup-report.md), `raw/resumed-inventory-failure.json`, `raw/resumed-inventory-matrix.json` and the exact native reports before further repair. No inventory or historical serialization cause is attributed to the zero-debt ordering repair.

## Owner handoff

1. Do not integrate this NOT_READY candidate. In the owner master session, verify branch master, a clean status, master `bd191a007b63a8ef94baccfb41f606ffec823ef1`, catchup equal to this packet's `final_commit`, and `git merge-base --is-ancestor bd191a007b63a8ef94baccfb41f606ffec823ef1 catchup` exit 0 before reviewing any later packet.
2. Resolve the unexempted inventory timeout through a demonstrated catch-up-only repair, or individually matching native failure evidence on exact current master. Next permitted work is a read-only comparison of the earliest differing ordinary construction/food/material service operands before the failed 8,654 wall-completion boundary, including worker/controller/nutrition inputs. No repair is inferred from isolated/cohort passes; preserve the original red. Do not change supplies, accepted current-stock predicates, horizons or ordinary gameplay rules to obtain green.
3. Reconcile later master changes and refresh evidence according to executable/runtime differences. Require an exact later READY packet, complete affected acceptance, fresh final fast/complete acceptance, assembly and only individually demonstrated named master exceptions, preserving any actual nonzero exits.
4. Only after READY, run `git merge --no-ff catchup` from the clean owner master session, then both unfiltered commands:

   ```powershell
   .\gradlew.bat checkFast -PprimeAntsGameTestConcurrency=2 --console=plain --rerun-tasks
   .\gradlew.bat check assemble -PprimeAntsGameTestConcurrency=2 --console=plain --rerun-tasks
   ```

5. If executable/runtime inputs advance beyond that certified packet, reconcile and refresh the relevant acceptance. A proved documentation-only advancement does not invalidate unchanged executable workloads; record correspondence explicitly.
