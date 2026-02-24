# AGENTS.md - NeteaseMusicDisplay

This file is the execution guide for coding agents working in this repository.

## Project Intent

NeteaseMusicDisplay is a Fabric client-side mod that renders the currently playing Netease Cloud Music track inside Minecraft.

- Primary runtime target: Minecraft `1.21.10`, Java `21`, Fabric Loader.
- Platform behavior: track detection is Windows-specific (`cloudmusic.exe` via JNA), but the mod must stay safe on non-Windows systems.
- User-facing contract: `/nmd` commands configure overlay state, position, color, width, and scale.

## Source Map (Current Code)

- `src/main/kotlin/com/as9929/display/netease/NeteaseMusicDisplay.kt`
  - Mod entrypoint. Loads config and registers client commands.
- `src/main/java/com/as9929/display/netease/mixin/InGameHudMixin.java`
  - Injects into HUD render to call `TextRender.onRender(...)`.
- `src/main/kotlin/com/as9929/display/netease/TextRender.kt`
  - Render pipeline + background polling thread (1s cadence) for song title cache.
- `src/main/kotlin/com/as9929/display/netease/CloudMusicHelper.kt`
  - Windows API access through JNA (`user32` / `kernel32`) to read title from `cloudmusic.exe` windows.
- `src/main/kotlin/com/as9929/display/netease/RenderUtils.kt`
  - Text drawing and scrolling logic with scissor clipping.
- `src/main/kotlin/com/as9929/display/netease/config/ModConfig.kt`
  - Config model + `ConfigManager` load/save/update behavior.
- `src/main/kotlin/com/as9929/display/netease/command/ConfigCommand.kt`
  - `/nmd` command tree and validation logic.

## Agent Priorities

1. Keep runtime behavior stable for players.
2. Preserve render-thread safety.
3. Preserve non-Windows safety.
4. Keep config compatibility and command UX intact.
5. Prefer small, reviewable diffs over broad refactors.

## Non-Negotiable Invariants

### Windows/JNA Safety

- Always gate Windows-only behavior with an OS check before any JNA call path.
- Keep native libraries lazily loaded (`by lazy`) so non-Windows startup does not fail.
- Close native handles in `finally` blocks.
- Expected failure path is `null` / no render, not a crash.

### Threading and Render Safety

- Do not run heavy process/window scans on the render thread.
- Shared mutable state between background polling and render code must remain thread-safe (`@Volatile` or stronger).
- Minecraft client access and drawing logic must stay on the render thread.

### Rendering Behavior

- `x == -1` means auto right-aligned placement.
- Maintain matrix push/pop balance and scissor enable/disable balance.
- Scrolling text should remain smooth and clipped to `maxBoxWidth`.

### Config and Command Contract

- Config file path/name is `config/netease-music-display.json`.
- Keep `ModConfig` defaults, `/nmd` argument bounds, and status output coherent after changes.
- If adding/changing config fields, keep backward compatibility (`ignoreUnknownKeys` is already enabled).

## Build and Validation

Use Gradle wrapper from repo root:

```bash
./gradlew build
./gradlew test
./gradlew check
```

Notes:
- Tests are currently absent; add them under `src/test/kotlin/...` when introducing non-trivial logic.
- For documentation-only edits, full build is optional.

## Coding Conventions

### Kotlin

- 4-space indentation, LF endings.
- Package: `com.as9929.display.netease`.
- Prefer immutable data and `copy(...)` updates for config.
- Prefer nullable return paths for expected runtime misses.
- Use SLF4J logging instead of stack trace prints in new code.

### Java/Mixin

- Keep mixins in `com.as9929.display.netease.mixin`.
- Use explicit `@Inject` targets and stable injection points.
- Avoid broad injections when a precise target is available.

### Imports and Naming

- Use explicit imports; avoid wildcard imports.
- Class/Object: PascalCase, method/property: camelCase, constants: UPPER_SNAKE_CASE.

## When Changing Specific Areas

### If you touch `CloudMusicHelper`

- Re-verify OS guard, lazy native loading, and handle cleanup.
- Keep noisy/error-prone native failures non-fatal.

### If you touch rendering (`TextRender` / `RenderUtils` / mixin)

- Re-verify no heavy work moved onto render path.
- Re-verify clipping, scale, and right-alignment behavior.

### If you touch config or commands

- Re-verify all `/nmd` subcommands:
  - `color`, `pos`, `width`, `scale`, `toggle`, `reset`, `status`
- Keep command validation messages clear and user-safe (clamping where needed).

### If you update versions/dependencies

- Update `gradle.properties` and ensure `build.gradle` remains consistent.
- Confirm `fabric.mod.json` dependency bounds are still accurate.

## PR/Change Checklist for Agents

- Change is scoped to the requested outcome.
- No regressions to non-Windows startup/runtime behavior.
- No render-thread blocking added.
- Command/config behavior remains coherent.
- Build/test status reported honestly (including if not run).

## Reference Links

- Fabric docs: https://fabricmc.net/wiki
- Mixin docs: https://github.com/SpongePowered/Mixin/wiki
