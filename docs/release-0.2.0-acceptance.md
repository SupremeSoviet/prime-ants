# Current 0.2.0 acceptance — T20, 2026-10-18

**BLOCKED / UNPUBLISHED.** The fresh complete suite has **385/386 server passes** and **1 nonexempt
result(s)**, including the miner named-unit recovery assertion. Qualifying documented timeouts: **0**.
Final fast: 77/77; unit/model: 124/26; Python harness: 28. All 386 identities execute exactly once in the final
fast+long union; all complete native/merged testcase elements match. Original raw exits remain, with
`checkLong` exit **1**. No diagnostic or historical pass substitutes for this failure.
The oversized-inventory identity passes in this fresh complete run; its exemption is not used and no cause or repair is inferred for T19.

The outer 60,000-tick timeout qualification for the exact miner identity does not exempt its 6,000-loaded-tick
assertion. [Blocker and bounded diagnosis](t20-release-blocker.md). The seven exact [known-flaky timeouts](known-flaky-timeouts.md)
retain original guards, bounds, failures and native/Gradle/root exits. The owner's 00:35 oversized qualification
supersedes only that exact 20,000-tick timeout; T19's first cause remains UNKNOWN and catchup6 actor-order findings stay separate.

Version inputs and both jar metadata are 0.2.0; production behavior and dependencies are unchanged. Sources-jar version
expansion was corrected, then targeted recovery and a fresh final fast passed before the one complete long run.
Runtime SHA-256: `bb3bbb2777b8db43d33e64001d4b7f90b94b75e196b3f71f7d5ff784405480a0`

Sources SHA-256: `729a2e9a09b5d8e2af01932f5fff5f715e96a675b9bc6e96b5d2d533ad7bad6a`

The artifacts are frozen **for review only** in external `T20/raw/review-release/`. They are not installed,
uploaded or downloaded as a release. Minecraft client starts: **0**; world openings: **0**; current Survival PNGs: **0**.
The supported Windows capture/input path was positively checked in a disposable task-owned UI window, but
client/gallery work was stopped behind the nonexempt test failure. No release commit/tag/push/publication is claimed.
Published 0.1.0 and its gallery remain historical; see [final prepared EN/RU notes](release-0.2.0.md).

The [T19 accepted functional persistence chain](t19-functional-persistence.md) is carried forward unchanged:
default-speed natural brood, simultaneous food/clay work, two fresh-process reopens, exact saved/first-decoded
state and resumed ordinary service. This remains graceful persistence for the exercised Young/Mature states,
not crash durability or broad modpack testing. Performance stays a [known T16 limitation](t16-cap-performance.md),
outside the gate; no performance work or T17 optimization was reinstated.

## Русский

**БЛОКИРОВКА / НЕ ОПУБЛИКОВАНО.** Свежий полный набор: **385/386** серверных проходов,
неосвобождённых результатов **1**, квалифицированных тайм-аутов **0**. Финальные fast 77/77, unit/model 124/26,
Python 28; все 386 имён и полные native/merged элементы сохранены. Исходный exit `checkLong`: **1**.
Новый miner assertion за 6 000 загруженных тактов не является разрешённым 60 000-тактовым timeout.
Первопричина не установлена; исправление или повторение ради зелёного не делались.
Oversized-тест прошёл в этом свежем полном наборе без применения исключения; причина T19 и её исправление из этого не выводятся.

Версия и оба jar — 0.2.0, зависимости не изменены. Файлы заморожены только для просмотра; не установлены
в текущий клиент и не опубликованы. Запусков клиента/открытий миров/текущих Survival PNG: **0/0/0**.
Историческая 0.1.0 не подменяет приёмку нового jar. Функциональная цепочка T19 перенесена без повторения;
это не устойчивость к авариям или широкая совместимость. Производительность остаётся ограничением T16 вне допуска.
