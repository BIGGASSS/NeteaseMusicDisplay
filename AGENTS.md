# Repository Guidelines

## Project Structure & Module Organization
- Source code is under `src/main/kotlin/com/as9929/display/netease/` (rendering, commands, config, and music-title lookup).
- Mixin and Java interop code lives in `src/main/java/com/as9929/display/netease/mixin/`.
- Mod metadata and assets are in `src/main/resources/`:
  - `fabric.mod.json`
  - `netease-music-display.mixins.json`
  - `assets/netease-music-display/`
- Build artifacts are generated in `build/libs/` (the CI workflow uploads jars from here).

## Build, Test, and Development Commands
- `./gradlew build`: compile, remap, and package the mod jar (primary CI command).
- `./gradlew runClient`: start a local Fabric client for manual verification.
- `./gradlew test`: run unit tests (currently no test sources are committed).
- `./gradlew clean`: remove generated build outputs.

## Coding Style & Naming Conventions
- Languages: Kotlin (primary) and Java (mixin integration).
- Use 4-space indentation in new/edited files; do not introduce tabs.
- Naming:
  - Classes/objects: `PascalCase`
  - Functions/properties: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- Keep packages under `com.as9929.display.netease...`.
- Favor immutable config updates (`copy(...)`) and safe concurrency patterns (`@Volatile`, background threads for blocking IO).

## Testing Guidelines
- Place tests in `src/test/kotlin/` using mirrored package paths.
- Name files `*Test.kt` and keep test names behavior-focused (example: `clampsYWhenBeyondScreenHeight`).
- Prioritize tests for command validation, config serialization, and render-position calculations.
- No coverage gate is configured; include tests for any non-trivial logic changes.

## Commit & Pull Request Guidelines
- Follow concise, imperative commit subjects used in history (for example: `Fix typo in README.md`, `Add support for in-game-command configuration`).
- Keep each commit scoped to one logical change.
- PRs should include:
  - What changed and why
  - How to test (`./gradlew build`, `./gradlew runClient`)
  - Screenshots or short clips for HUD/rendering changes
  - Linked issue(s) when applicable
- Call out Minecraft/Fabric version impacts if compatibility changes.
