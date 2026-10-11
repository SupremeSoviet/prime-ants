# Stage 1: verified partial delivery

**Partial Stage-1 delivery; Great remains valid-red with unresolved cause; release 0.2.0 is not accepted.**
The 0.1.0 physical engine supports useful parts of the first fantasy layer: live stages, functional chambers,
physical tier-two work, supplied mining and contributions, and completed Mature surface structures in controlled
tests. It has not demonstrated the complete player-facing development cycle. The twenty-turn direction closes as
partial; [published 0.1.0](slice-1-report.md) remains separate from unreleased work on `master`.

The table distinguishes implemented behavior and controlled acceptance from historical observations, failures and
untested coverage. Test/source identities and external turn evidence are indexed in the [short appendix](stage-1-report-appendix.md).

| Useful-outcome requirement | Evidenced delivery | Failure or untested remainder |
|---|---|---|
| 1. Live stages and visible growth | [StageRules](../src/main/java/dev/primeants/colony/StageRules.java) derives stages from living adults, confirmed chambers and physical stores; caps **5/30/60/120**, including queen. `ColonyStageGameTest` covers promotion, regression and reload; T18 passes these bodies. | The configured 120 cap demonstrates neither a 120-adult colony nor performance. Great's complete visible form fails. |
| 2. Four chamber functions and tiers | Nursery, food store, material store and queen's hall; actual worker clay hauling/wall conversion and tier-two capacity, development and laying effects in [TierTwoGameTest](../src/gametest/java/dev/primeants/gametest/TierTwoGameTest.java), accepted in T18; earlier scoped T08 evidence. | **Tier-three construction and effects remain unmet.** Stone-wall recognition and block registration do not deliver them. |
| 3. Mining and player contributions | [MiningGameTest](../src/gametest/java/dev/primeants/gametest/MiningGameTest.java) and [MaterialHaulingGameTest](../src/gametest/java/dev/primeants/gametest/MaterialHaulingGameTest.java): supplied gallery, physical trips, stores, natural-origin protection, named custody and conservation; T18 controlled positives, T09–T12 history. | Fixed sixteen-cell gallery; arbitrary veins and all 64 permitted edits are untested. Earlier preparation/timing failures remain adverse evidence. |
| 4. Plan-based surface structures | [SurfaceGameTest](../src/gametest/java/dev/primeants/gametest/SurfaceGameTest.java): controlled Mature **19/19**, real passage and 200 settled loaded ticks; T18 supplied founder **19/19 at 44,978**. | T19 Great: receipt prefix **43/57**, physical form **47/57**; six post-step and four upper watch-post cells missing. Complete form, walking passage and 200 settled loaded ticks were not achieved. |
| 5. Established world generation | Existing natural founding has controlled coverage; historical 0.1.0 observations keep their labels. | Established Young/Mature colonies with pre-dug plan-based nests remain unmet. |
| 6. Performance and persistence | Scoped registry, inventory, worker-cargo and normal disk/data reload tests pass in T18; these are controlled positives. | Great/120-adult and multiple-colony performance, broad persistence and abrupt-crash durability remain unmet. Command duration is not colony MSPT. |
| 7. Release 0.2.0 | T20 documentation and fresh fast acceptance verify a partial handoff; current branch jars retain version **0.1.0** as unreleased master artifacts. | Complete build acceptance remains red. No fresh Stage-1 clean-client acceptance or required Survival screenshot set; the existing 0.1.0 gallery retains its historical labels. Packaging is not release acceptance. |

**Acceptance is scoped.** T18 remains the complete infrastructure-valid, gameplay-red checkpoint: **267 server
identities**, comprising **73/73 fast + 193/194 long**, plus **115 unit / 26 model**; Great alone times out at
**12,000 ticks**. T19's infrastructure-valid bounded diagnostic remains red, cause unresolved: **79 soil blocks +
1 canonical cargo = 80**, stone **32**. Its separate fast acceptance passes **73/115/26**, with **26 Python harness tests**.
T20's sole fresh `checkFast assemble` passes **73 fast / 115 unit / 26 model / 26 Python harness tests** in **120.243s**,
with unfiltered XML, all guards, protected clients and jars matching T19. This duration is command wall time.
Fast acceptance and packaging/protection verification are settled in external local evidence
`<turnloop>/directions/prime-ants-stage1/turns/T20/raw/stage-1-handoff.json`; exact commands and hashes are outside this summary.

**Clock correction:** first certain Great is helper **100**, nursery-loaded **101**; Mature completion is helper
**4,256**, Great admission **4,301**. The endpoint is a failure-listener observation at **12,002**, following the unchanged
12,000-tick timeout. Automatic observations at **12,000/12,001** are excluded from the **9,250** bounded controller total.
Movement-request counts, supported stands and path metadata do not establish a navigation cause. T19's archived appendix is unchanged.

T18's founder positive does not replace T17 selected **44,720**, simultaneous **45,290**, cohort **5/6**, or T16's
priority-cancelled **4/19**, **71 owned blocks plus its named unit**, operation two never started. Earlier mining
preparation/timing uncertainties, the unexplained eighteen-unit failure at **10,323**, paid-stall/persistence gaps,
mixed chunk-boundary issues, unattended pre-Mature clay/development and survey walking/dig limits remain in the appendix.

Recommend a **bounded Stage-1 completion follow-up before forging, armor or diplomacy**, beginning with Great's unresolved
deadline failure and preserving adverse evidence and existing physical limits. No particular repair is promised.
