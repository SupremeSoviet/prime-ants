# Prime Ants

[![Build](https://github.com/SupremeSoviet/prime-ants/actions/workflows/build.yaml/badge.svg)](https://github.com/SupremeSoviet/prime-ants/actions/workflows/build.yaml)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62B47A)
![Fabric](https://img.shields.io/badge/Fabric-0.19.2-DBD0B4)
![Java](https://img.shields.io/badge/Java-21%2B-ED8B00)
![License](https://img.shields.io/github/license/SupremeSoviet/prime-ants)

Prime Ants is a Fabric mod for Minecraft focused on playable ant colonies: castes, chambers, resources, research, diplomacy, trades, and raids.

The internal mod id is `formic_frontier`.

## Features

- Large ant castes with visible nest structures.
- Colony economy built around food, ore, chitin, resin, fungus, venom, and knowledge.
- Research, diplomacy, trading, logistics requests, and rival colonies.
- Synced Colony Tablet UI plus command-driven debugging.
- Gradle build, unit tests, server GameTests, and GitHub Actions CI.

## Requirements

| Tool | Version |
| --- | --- |
| Minecraft | `1.21.11` |
| Fabric Loader | `0.19.2` |
| Fabric API | `0.141.3+1.21.11` |
| Java | `21+` |

## Development

```bash
./gradlew build
./gradlew runClient
```

Useful test commands:

```mcfunction
/formic colony create
/formic colony dump
/formic ant spawn worker
```

## License

MIT.
