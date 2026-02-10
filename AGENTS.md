# AGENTS.md - NeteaseMusicDisplay

A Minecraft Fabric mod that displays the current playing song from Netease Music on Windows.

## Tech Stack

- **Build System**: Gradle with Fabric Loom
- **Primary Language**: Kotlin (2.3.0)
- **Secondary Language**: Java (for Mixin classes)
- **Java Version**: 21
- **Minecraft Version**: 1.21.10
- **Mod Loader**: Fabric

## Build Commands

```bash
# Build the mod JAR
./gradlew build

# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "com.as9929.display.netease.YourTestClass"

# Run a specific test method
./gradlew test --tests "com.as9929.display.netease.YourTestClass.testMethod"

# Run all checks (includes tests, ABI checks)
./gradlew check

# Clean build directory
./gradlew clean

# Full clean build
./gradlew clean build

# Generate sources JAR
./gradlew sourcesJar
```

## Project Structure

```
src/
├── main/
│   ├── java/com/as9929/display/netease/mixin/    # Java Mixin classes
│   ├── kotlin/com/as9929/display/netease/        # Kotlin source
│   └── resources/                                 # Mod metadata, assets
│       ├── fabric.mod.json
│       └── netease-music-display.mixins.json
├── test/                                          # Test sources (if any)
build.gradle                                      # Build configuration
gradle.properties                                 # Version constants
settings.gradle                                   # Project settings
```

## Code Style Guidelines

### Kotlin

- **Indentation**: 4 spaces (no tabs)
- **Line endings**: LF
- **Package**: `com.as9929.display.netease`
- Use `object` for singletons (e.g., `object TextRender`)
- Use `by lazy` for expensive initialization
- Prefer `val` over `var`
- Use nullable types (`String?`) over exceptions for optional returns
- Comments: Use `// --- Description ---` for section headers

Example:
```kotlin
package com.as9929.display.netease

import net.fabricmc.api.ModInitializer

object NeteaseMusicDisplay : ModInitializer {
    const val MOD_ID = "netease-music-display"
    
    override fun onInitialize() {
        // Initialization code
    }
}
```

### Java

- **Indentation**: 4 spaces
- **Package**: `com.as9929.display.netease.mixin` for Mixin classes
- Use Mixin pattern for Minecraft injection points
- Annotations on separate lines for complex injections

Example:
```java
package com.as9929.display.netease.mixin;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(
        method = "render",
        at = @At(value = "INVOKE", target = "...")
    )
    private void onRender(...) { }
}
```

### Naming Conventions

- **Classes**: PascalCase (e.g., `CloudMusicHelper`, `TextRender`)
- **Objects**: PascalCase (e.g., `TextRender.INSTANCE`)
- **Methods/Functions**: camelCase (e.g., `getCloudMusicTitle()`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MOD_ID`, `PROCESS_QUERY_INFORMATION`)
- **Packages**: lowercase (e.g., `com.as9929.display.netease`)

### Imports

- Group imports by source:
  1. Kotlin/Java standard library
  2. Minecraft/Fabric libraries
  3. Project imports
  4. Third-party libraries (JNA, etc.)
- Use wildcard imports sparingly

### Error Handling

- Use nullable return types instead of throwing exceptions for expected failures
- Wrap platform-specific code (Windows API) with OS checks first
- Use try-finally for resource cleanup (handles, processes)
- Log errors using SLF4J: `LoggerFactory.getLogger(MOD_ID)`

### Threading

- Offload heavy work to background threads using `Executors`
- Mark shared mutable state with `@Volatile`
- Use daemon threads: `t.isDaemon = true`
- Always access Minecraft client on main render thread only

### Mixin Guidelines

- Place all Mixin classes in `mixin` package
- Use `@Inject` for adding behavior
- Use proper `@At` targets (e.g., `INVOKE`, `HEAD`, `TAIL`)
- Include shift when needed: `shift = At.Shift.BEFORE`

## Key Dependencies

- Fabric Loader: `0.18.4`
- Fabric API: `0.138.4+1.21.10`
- Fabric Language Kotlin: `1.13.8+kotlin.2.3.0`
- JNA: For Windows API access (cloudmusic.exe integration)

## Testing

Currently no tests exist. When adding tests:
1. Place in `src/test/kotlin/` mirroring main package structure
2. Use JUnit 5 (included via Gradle)
3. Run with `./gradlew test`

## CI/CD

GitHub Actions workflow at `.github/workflows/build.yml`

## Resources

- Fabric Wiki: https://fabricmc.net/wiki
- Mixin Wiki: https://github.com/SpongePowered/Mixin/wiki
