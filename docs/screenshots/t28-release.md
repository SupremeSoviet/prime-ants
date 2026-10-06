# Release illustrations / Иллюстрации выпуска

Four illustrations: three accepted current original game PNGs plus the authorized historical queen fallback, opened individually. **The queen uses the previous model revision; current queen visual acceptance remains incomplete.** / Четыре иллюстрации: три принятых текущих игровых снимка и разрешённый исторический снимок матки, открытые отдельно. **Матка использует предыдущую редакцию модели; визуальная приёмка нынешней матки остаётся незавершённой.**

| View / Вид | Image | Caption and status |
|---|---|---|
| Entrance / Вход | [Entrance and mound](t28-release-entrance.png) | **Accepted.** Naturally excavated entrance and deposited mound; daylight, hidden HUD, survival camera on an existing supported mound. / Принят: естественный вход и выброшенный грунт, дневной свет, скрытый интерфейс, камера на существующей опоре. |
| Foraging / Фуражировка | [Genuine forager](t28-release-forager.png) | **Accepted.** One genuine worker on the clear entrance trail; ordinary daylight command, spectator camera, hidden HUD. / Принят: один настоящий фуражир у входа; дневной свет, камера наблюдателя. |
| Nursery / Ясли | [Living egg](t28-release-interior.png) | **Accepted for living brood.** The white object near the center is an egg, not the pale adult in front. One survival-placed wall torch, gamma0.5/FOV70, no night vision; daylight can enter the opened nest. The egg is unobscured; surrounding ants are cropped. / Принят для живого выводка: белый предмет у центра — яйцо, а не светлый взрослый впереди. Один настенный факел, обычная яркость, без ночного зрения; возможен дневной свет. Яйцо не закрыто, окружающие муравьи обрезаны. |
| Queen / Матка | [Historical daylight queen](t25-appearance-a5-queen-queen-daylight.png) | **T25 — previous model revision. / T25 — предыдущая редакция модели.** Accepted historical natural founding queen carrying soil; unchanged PNG supplies illustration four, not current queen acceptance. / Принятый исторический вид естественной матки с грунтом; неизменённый снимок даёт четвёртую иллюстрацию, но не приёмку текущей матки. |

The [T28 attempted original](rejected/t28/t28-queen-clipped.png) remains **rejected** for right-edge clipping; its later preview was terrain-obscured. T29 used the authorized unavailable branch because cheap live tick/early-close enforcement was not established: zero starts, openings or new PNGs. / [Оригинал T28](rejected/t28/t28-queen-clipped.png) остаётся **отклонённым** из-за обрезки справа; следующий предварительный вид закрыт рельефом. В T29 применён разрешённый вариант без новой съёмки: простой живой контроль тиков и раннего закрытия не обеспечен; новых запусков, открытий и снимков нет.

## Current-image capture qualifications

The three T28 accepted files are unedited1280×800 native F2 images with the final relevant production/assets inputs. Source: task-owned `T23/t23-player-a5-world.zip`, root `t23-player-a5/`, natural queen `9c71829b-25f1-3b96-acbb-96252228271d`, entrance `(168,67,149)`. Historical founding used work20/brood100. This JVM uses default work1/brood1 and ordinary20TPS, render/simulation8/8. The nursery's saved stage duration120 and existing callow durations retain prior acceleration. No biological clock, stage, reserve, nutrition, ant position, AI or cargo assignment was edited.

The interior egg is fresh food-fed brood `f6a487e0-5ae7-44c3-8a42-35ae6a7175a1`: laid18:17:04, photographed18:17:06, natural larval transition18:17:10. Four apples and two raw chicken were supplied once through declared survival-player inventory and ordinary drop keys. Logs record collection, delivery and nursing. Short backward-key inputs did not move the observer, so **immediate ordinary retreat was not demonstrated**; later camera relocation moved away. No food renewal. One of two available torches was normally placed at `(169,66,146)` on the existing wall; the unused torch later dropped on observer death. No terrain clearing.

The outdoor ant is consistent with sole registered trail forager `9a5b931b-bb9b-332d-b42a-34a6202eccca`, original brood `0307320e-4be0-47f1-a5e8-c51569e5ae9b`; entity readback and harvest/delivery traces support attribution. Native F2 supplies no render UUID binding, so frame-specific attribution is inferential. All pictured workers are genuine colony members. Feeding does not establish unattended growth or walk-from-spawn discovery.

The preferred torch-lit source's cocoon emerged normally after opening. The earlier T23 source's larva also developed with already-carried food. Neither old identity supplied the final living-brood image. The rejected queen attempt used the declared T22 founding save. Original archives are preserved.

One physical client start, three world openings, exit0 and immediate archives. **20,075 additional saved server ticks exceed the declared20,000 limit by75.** No reinterpretation or further capture. Native world-generation lag and a vanilla zombie killing the observer are preserved; no client crash or JIT workaround occurred. The populated session does not replace final clean-client verification.

Originals: interior `2026-10-06_18.17.06.png`; entrance `2026-10-06_18.19.45.png`; forager `2026-10-06_18.26.00.png`; optional wider forager `2026-10-06_18.27.11.png` remains archived. The first forager was provisionally judged from a later UI view; opening its actual PNG corrected that judgement: its entire body is in frame. Queen `2026-10-06_18.31.54.png` is rejected.

[Player instructions](../../README.md) · [Technical report](../slice-1-report.md) · [Turn evidence](../../../turnloop/directions/prime-ants-slice1/turns/T28/report.md)

## Historical queen qualifications / Условия исторического снимка матки

**T25 — previous model revision. / T25 — предыдущая редакция модели.**

![Historical natural queen in daylight](t25-appearance-a5-queen-queen-daylight.png)

Original accepted 1600×1000 daylight PNG from the T22 founding source; ordinary Respawn for its saved dead observer, camera-only framing, FOV55 and about3.595 blocks focus distance. No queen movement, forced pose, terrain clearing, crop or retouch. T25 used a dev-only exact-method client JIT exclusion after a native C2 crash; this historical image is not clean current-client verification. T26 subsequently revised the model. [Original review](t25-appearance-review.md) and [queen capture metadata](t25-appearance-a5-queen-appearance.json) retain the detailed evidence.

Оригинальный принятый дневной PNG1600×1000 из сохранения основания T22: обычное возрождение сохранённого погибшего наблюдателя, перемещение только камеры, FOV55 и расстояние около3,595 блока. Матка не перемещалась, поза не задавалась, рельеф не расчищался; обрезки и ретуши нет. В T25 после нативного сбоя C2 применялось исключение одного метода из JIT только для среды разработки; исторический снимок не является проверкой нынешнего чистого клиента. В T26 модель затем изменилась.
