# Infrastructure captures

![T01 infrastructure capture; colony not implemented](t01-infrastructure.png)

**T01 infrastructure capture; colony not implemented**

Automatically captured by Fabric client GameTest in a freshly generated normal survival world, seed `2026100401`, at the natural spawn/camera. No terrain editing, decorations, forced poses, or demonstration colony. The image was opened and observed to contain snowy terrain, spruce trees, sky, the survival HUD, and the player's hand. This is a rendered-gameplay observation, not a visual acceptance verdict. It satisfies none of the four eventual colony views.

- Command: `.\gradlew.bat runClientGameTest --console=plain`; exit `0`; client attempt `1`.
- Run start: `2026-10-04T14:55:02.0947520+05:00`.
- Capture time: `2026-10-04T09:56:11.695577400Z`.
- Wait for downloaded/rendered chunks: `42` ticks, then `5` additional ticks before capture.
- Path: `docs/screenshots/t01-infrastructure.png`.
- Dimensions: `1280 × 720`; size: `1,118,552` bytes.
- SHA-256: `0a8a35b2fece3dd9dcbf5142dba3c3ea4e037ff2cd75097ca35d3f49b9508609`.
- Provenance and complete logs: `C:\Users\user\Documents\turnloop\directions\prime-ants-slice1\turns\T01\capture-provenance.json` and `09-client-capture-attempt-1.{json,log}`.


## T02 debug entity specimens; colony not implemented

The final evidence is client attempt 4 in a fresh normal generated world, seed `2026100402`. These are production debug adults, not a brood-produced population or a completed colony close-up. Natural stone ground, terrain edges, trees and ore were left intact. Both adults retain AI and physics. The observer switches to spectator, follows each adult, hides HUD and uses FOV 45. No ant teleport, terrain edit, decoration, time/weather change or forced animation pose occurs.

![T02 debug entity specimens; colony not implemented — worker](t02-a4-worker-walking-2.png)

![T02 debug entity specimens; colony not implemented — queen](t02-a4-queen-walking-2.png)

Each PNG is 1600×1000. The three walking frames per form are separated by real game ticks and make changing leg positions inspectable; independent visual acceptance is pending. Full creation/movement records: [attempt 4 provenance](t02-capture-provenance.json). Earlier records are preserved in [attempt 2 provenance](t02-a2-capture-provenance.json) and [attempt 3 provenance](t02-a3-capture-provenance.json).

- `t02-a3-worker-walking-1.png` — **T02 debug entity specimens; colony not implemented**; worker, walking frame 1, attempt 3: isolated worker observation; run later failed waiting for the queen.
- `t02-a3-worker-walking-2.png` — **T02 debug entity specimens; colony not implemented**; worker, walking frame 2, attempt 3: isolated worker observation; run later failed waiting for the queen.
- `t02-a3-worker-walking-3.png` — **T02 debug entity specimens; colony not implemented**; worker, walking frame 3, attempt 3: isolated worker observation; run later failed waiting for the queen.
- `t02-a4-queen-walking-1.png` — **T02 debug entity specimens; colony not implemented**; queen, walking frame 1, attempt 4: final autonomous walking close-up.
- `t02-a4-queen-walking-2.png` — **T02 debug entity specimens; colony not implemented**; queen, walking frame 2, attempt 4: final autonomous walking close-up.
- `t02-a4-queen-walking-3.png` — **T02 debug entity specimens; colony not implemented**; queen, walking frame 3, attempt 4: final autonomous walking close-up.
- `t02-a4-worker-walking-1.png` — **T02 debug entity specimens; colony not implemented**; worker, walking frame 1, attempt 4: final autonomous walking close-up.
- `t02-a4-worker-walking-2.png` — **T02 debug entity specimens; colony not implemented**; worker, walking frame 2, attempt 4: final autonomous walking close-up.
- `t02-a4-worker-walking-3.png` — **T02 debug entity specimens; colony not implemented**; worker, walking frame 3, attempt 4: final autonomous walking close-up.
- `t02-queen-walking-1.png` — **T02 debug entity specimens; colony not implemented**; queen, walking frame 1, attempt 2: initial mixed adult observation; queen partly occludes worker.
- `t02-queen-walking-2.png` — **T02 debug entity specimens; colony not implemented**; queen, walking frame 2, attempt 2: initial mixed adult observation; queen partly occludes worker.
- `t02-queen-walking-3.png` — **T02 debug entity specimens; colony not implemented**; queen, walking frame 3, attempt 2: initial mixed adult observation; queen partly occludes worker.
- `t02-worker-walking-1.png` — **T02 debug entity specimens; colony not implemented**; worker, walking frame 1, attempt 2: initial mixed adult observation; queen partly occludes worker.
- `t02-worker-walking-2.png` — **T02 debug entity specimens; colony not implemented**; worker, walking frame 2, attempt 2: initial mixed adult observation; queen partly occludes worker.
- `t02-worker-walking-3.png` — **T02 debug entity specimens; colony not implemented**; worker, walking frame 3, attempt 2: initial mixed adult observation; queen partly occludes worker.

- **Worker** UUID `b6dbc54f-b790-40fb-856a-d998af428dd1`; creation route: `ordinary operator /summon prime_ants:lasius_niger_worker -14.50 69.00 6.50`. Created at server game tick 61, elapsed age 1. Walking frames at server ticks 96, 102, 108 / ages 36, 42, 48. From first to third frame, horizontal displacement 0.8424 blocks in 12 ticks. Positions [-14.399417238711827, 69.0, 6.600574659240077] → [-13.803730328901468, 69.0, 7.196259994981051]. AI, ground contact and normal physics observed active in all frames.
- **Queen** UUID `f428e324-bddc-48cf-95c1-cc2bb00a9ec5`; creation route: `production queen egg: client useItemOn -> packet -> ItemStack.useOn -> SpawnEggItem`. Created at server game tick 59, elapsed age 1. Walking frames at server ticks 116, 122, 128 / ages 58, 64, 70. From first to third frame, horizontal displacement 0.3739 blocks in 12 ticks. Positions [-9.560473028457066, 73.0, -12.490382755233302] → [-9.934329113352671, 73.0, -12.492358325157618]. AI, ground contact and normal physics observed active in all frames.

| Final image | SHA-256 |
| --- | --- |
| `t02-a4-queen-walking-1.png` | `bccda1bf50ba5e20a5769cb5ccfea42d96c45eacaa183f2d1735fa430923f6b8` |
| `t02-a4-queen-walking-2.png` | `993a592f7038afd5e1e149b64c93e50263d6cc0e6c11c8d0a3272ce95ea3f910` |
| `t02-a4-queen-walking-3.png` | `1a95272854dde5d6486b8e94498264ca1e55c6d1174d3c0c4b0b2ab94950a8f3` |
| `t02-a4-worker-walking-1.png` | `c3e9ce3629dcdd0183a3edf4ed0b96cd673140275c76691836ed4ce3951d8b13` |
| `t02-a4-worker-walking-2.png` | `d8445053fdfc0821128d14654ea66a5abf3dfe607dac060e8175adcad6d2d8fb` |
| `t02-a4-worker-walking-3.png` | `fe91c302d9df5989298e52648a63b0ae27f9b31f1c80c36a5dfc83cb62bafd15` |

Capture history: attempt 2 exit 0 (29.886 s, composition concern); attempt 3 exit 1 (43.397 s, queen idle-goal bug); server reproducer failed before correction and passed after correction; attempt 4 exit 0 (26.189 s). T01 remains byte-identical. All earlier captures are retained; no image is a colony acceptance claim.
