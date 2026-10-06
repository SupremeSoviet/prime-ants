# Current slice status - T23, 2026-10-06

**The real-client player checkpoint is complete on one naturally placed colony.**

| Stage | Result and evidence |
|---|---|
| Natural colony selected | Primary queen `9c71829b-25f1-3b96-acbb-96252228271d`, live20HP, genuine placement, entrance(168,67,149), ordinary seed2026100501 overworld; fallback never used. |
| Watching | 100 ordinary survival ticks without alarm; [entrance/mound and living worker](screenshots/t23-player-a5-entrance.png). |
| Entry and return | Ordinary movement/jump inputs through existing worker-opened stairs into the chamber and back; no measured teleport/noclip; [interior](screenshots/t23-player-a5-interior.png). |
| Food collected/delivered | Required one apple in A3 and optional one raw chicken in A5: inventory loss, world UUID, genuine forager cargo, owned cache, no recollection, immediate4.115m retreat; [visible chicken at mandibles](screenshots/t23-player-a5-feeding.png). |
| Actual client bite | After real survival attack, owned worker `ec9d7a07-ea92-396a-a736-38ed3d707167` gives source`mob`, health20->19, then ordinary retreat; [defense](screenshots/t23-player-a5-defense.png). |
| Readable images | All11 fresh PNGs opened. Four A5 frames accepted individually; A3 empty-handed feeding and A4 grass-occluded nectar remain explicitly rejected. [Review](screenshots/t23-player-a5-image-review.json). |
| Normal closure | All five owned attempts saved/closed and archived immediately before another Gradle task; final A5 copy retained for visual work. |
| Current build | Exact unfiltered rerun build passes15 unit/195 server/26 model, zero failures/errors/skips, all baseline names,448 unchanged inputs and both JAR exclusions. [Verification](../../turnloop/directions/prime-ants-slice1/turns/T23/final-verification.json). |

`verifyFreshPlayerCheckpoint` exits0 after image review and required same-colony apple-trace verification.
Loading, recovery-only, incomplete maturation, observer failure and completed interactions with unreviewed images remain distinct;
normal save/exit alone cannot pass. A3/A5 raw client tasks exit1 while images are unreviewed, preserving their historical exits.

The owner-authorized known-site route reopens an owned closed T22 A4 **client-created vanilla overworld**;
it is local access, not a walk-from-spawn discovery claim. Primary founding advances from3 removals to24,22 queen deposits,
three genuine first-clutch workers and their opened entrance. Founding loaded ticks488->6514; actual worker brood IDs match
the original clutch. Work20/brood100 acceleration preserves persisted stage120, costs, maintenance, life and source timing.
No supplied ants/brood, manual biology ticks, reserve/stage/AI/ant-position/target edits or player terrain clearing.
Player food is an interaction supply, not autonomous-intake evidence.

Observer setup uses spectator for loading/travel/waiting, actual non-generating FULL terrain before height/support reads,
build height/border, solid dry support, empty feet/head fluid, full0.6x1.8 body clearance, liveness and advancing client/server ticks.
Survival stand(168,67,152) is rechecked before physics/inputs. A1 uses the normal Respawn button for the saved dead T22 player;
no health edit. Measured entry, drops/retreats and attacks remain survival. A5 waits318 normal loaded ticks for the prior real alarm,
without clearing it. Daylight, hidden HUD, camera setup and fixed native1600x1000 are disclosed.

**12,029 /24,000 total natural ticks**, including the prior3,002 and all T23 recovery/load/wait/interaction/closure;
11,971 remain. Client history35 launches, native startup26/9, raw tasks15/20; one accepted full checkpoint and four accepted images
are separate results. Ten T23 headless server launches are separate(43 including the brief's33).

Russian guide recovered from Git UTF-8 bytes; final strict UTF-8/Cyrillic/no-replacement verification is in T23/guide-utf8.json.
Two variable controlled fixtures changed **supply order only**, preserving quantities, bounds, real actions and assertions;
their failed full runs, original narrow reproducers and targeted recoveries are retained. Native frame and equipment races were
diagnosed before renewed work; no timeout/RAM increase, recorder-guard widening or production mechanic change.

T22 A4 drowned its observer and read unavailable height-64; its original task exit is preserved, but its nonselection is observer
failure, not absence proof. [T22 historical report](t22-status-historical.md), [T23 details/commands](t23-player-checkpoint-appendix.md),
[turn report](../../turnloop/directions/prime-ants-slice1/turns/T23/report.md).
**T24-T26 remains appearance; performance and broad save/reload belong to T27-T30 release checks.**
