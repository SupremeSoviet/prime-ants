# 0.2.0 release notes — draft, 2026-10-17

Unreleased. The version remains 0.1.0 until the remaining acceptance work is complete. Controlled Great
fortifications, tier-three construction/effects and established Young/Mature generation have accepted coverage.
Persistence, clean-client acceptance, Survival screenshots and publication remain pending.

Several loaded Great colonies can cause substantial lag. T16's controlled development-server measurements:

| Loaded scene | TPS | Mean tick | p95 tick |
|---|---:|---:|---:|
| One Great colony, 120 adults | about 19 | about 52 ms | about 66 ms |
| Three Great colonies, 360 adults | about 6.7 | about 149 ms | about 216 ms |

Measured on Ryzen 7 7700 (8 cores/16 threads), Windows 11 Pro build 26200, approximately 95.1 GiB RAM,
Temurin 25.0.3+9 with G1 and a 3 GiB server heap; Minecraft 26.3, Loader 0.19.5, Fabric API 0.161.0+26.3,
Loom 1.18.2 development runtime. Shared-machine contention and test instrumentation remain included.
Each scene has 600 warm-up and 2,400 measured ticks, ordinary 20 TPS request, work/brood multipliers 1/1,
and a controlled supplied population. These measurements are not universal hardware guarantees or proof of
sustainable natural growth. The original mean <=40 ms and p95 <=50 ms budgets failed. T17's profiled diagnostic
timings are not release measurements. Performance is a documented 0.2.0 limitation under the owner's
2026-10-17 redirect; further work follows in a separate direction. See [T16 evidence and conditions](t16-cap-performance.md).

## Русский текст для выпуска

Черновик, выпуск ещё не опубликован. Приняты контролируемые проверки укреплений великой колонии,
строительства и эффектов третьего уровня и генерации готовых молодых/зрелых колоний. Приёмка сохранений,
чистого клиента, кадры из выживания и публикация ещё впереди.

Несколько одновременно загруженных великих колоний могут вызывать существенные лаги. Контролируемые замеры T16
на сервере разработки: одна великая колония со 120 взрослыми — около 19 TPS, среднее 52 мс, p95 примерно 66 мс;
три колонии с 360 взрослыми — около 6,7 TPS, среднее 149 мс, p95 примерно 216 мс.
Машина: Ryzen 7 7700, Windows 11 Pro build 26200, около 95,1 ГиБ ОЗУ, Temurin 25.0.3+9/G1, куча 3 ГиБ;
Minecraft 26.3, Loader 0.19.5, Fabric API 0.161.0+26.3, Loom 1.18.2. Это конкретная среда разработки,
а не универсальная гарантия для любого оборудования. Замеры включают нагрузку общей машины и тестовое
наблюдение; популяция задана контролируемой подготовкой, устойчивый естественный рост не доказан.
Исходные бюджеты среднего 40 мс и p95 50 мс не выполнены. Производительность остаётся документированным
ограничением 0.2.0; дальнейшая работа над ней пойдёт отдельным направлением и не задерживает этот выпуск.
