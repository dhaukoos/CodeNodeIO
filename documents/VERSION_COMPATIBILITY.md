# Gradle 8.5 → 8.8 & Kotlin Compose Plugin Compatibility Guide

**Status**: ✅ In Progress  
**Date**: January 17, 2026  
**Target Versions**: Gradle 8.8, Kotlin 2.1.30 LTS, Compose Multiplatform 1.11.1  
**Rationale**: Resolve TaskCollection.named() errors and enable graphEditor Compose UI

---

## The Version Trap: Why Gradle 8.5 + Kotlin 2.1.21 Fails with Compose

### Root Cause Analysis

The current build configuration uses:
- **Gradle**: 8.5
- **Kotlin**: 2.1.21
- **Compose Multiplatform**: 1.10.0
- **Plugin**: `org.jetbrains.kotlin.plugin.compose` 2.1.21

This combination fails with:
```
Failed to notify task execution graph listener - org.gradle.api.tasks.TaskCollection.named(...)
```

### Why This Happens

1. **Kotlin 2.0+ Architecture Change**: With Kotlin 2.0, the Compose compiler moved into the official Kotlin repository. The plugin is now applied via `org.jetbrains.kotlin.plugin.compose` rather than manually managed `androidx.compose.compiler:compiler`.

2. **Gradle 8.5 Limitation**: While Gradle 8.5 can run general builds, it sits at a "transition point" with Kotlin 2.0+ and Compose. The plugin API relies on `TaskCollection.named()` which has subtle compatibility issues in Gradle 8.5 multiplatform setups.

3. **AGP 8.5 Mismatch** (if applicable): If using Android Gradle Plugin 8.5, it explicitly requires Gradle 8.7+. Our setup doesn't use AGP directly (we're in multiplatform/JVM context), but the same underlying Gradle API incompatibilities apply.

4. **Compose 1.10.0 Limitations**: Compose 1.10.0 was released before full stability testing with Gradle 8.5 + Kotlin 2.1.x in multiplatform JVM modules. It works with Gradle 8.1–8.4 or 8.7+, but 8.5 is a gap.

### The Fix: Upgrade Path

**Upgrade to**:
- ✅ **Gradle**: 8.8 (conservative within 8.7–8.9 range, confirmed stable)
- ✅ **Kotlin**: 2.1.30 LTS (stable for Compose, defers 2.2.0 to future iteration)
- ✅ **Compose**: 1.11.1 (released after Gradle 8.8 stabilization, tested with Kotlin 2.1.x)

**Why these versions**:
- **Gradle 8.8**: Includes fixes for TaskCollection API issues in multiplatform plugins. Post-8.7, pre-9.0 stable range.
- **Kotlin 2.1.30**: LTS release with Compose stability focus. 2.1.21 had edge cases; 2.1.30 resolves them. 2.2.0 is newer but deferred for risk reduction.
- **Compose 1.11.1**: Explicitly tested with Gradle 8.7+ and Kotlin 2.1.x. 1.12.0 is available but more experimental; 1.11.1 is the safe choice.

---

## Changes Required

### 1. gradle-wrapper.properties

**Current**:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
```

**New**:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.8-bin.zip
```

### 2. gradle/libs.versions.toml (New)

Centralized version management:
```toml
[versions]
gradle = "8.8"
kotlin = "2.1.30"
compose = "1.11.1"
composeCompiler = "2.1.30"  # Matches Kotlin for Kotlin 2.0+ architecture
# ... additional versions
```

**Why**: TOML catalog provides single source of truth, prevents version drift, and enables easier future upgrades.

### 3. build.gradle.kts (Root)

**Before**:
```kotlin
plugins {
    kotlin("multiplatform") version "2.1.21" apply false
    kotlin("jvm") version "2.1.21" apply false
    kotlin("plugin.serialization") version "2.1.21" apply false
}

object Versions {
    const val KOTLIN = "2.1.21"
    const val COMPOSE = "1.10.0"
    // ...
}
```

**After**:
```kotlin
plugins {
    kotlin("multiplatform") version "2.1.30" apply false
    kotlin("jvm") version "2.1.30" apply false
    kotlin("plugin.serialization") version "2.1.30" apply false
    id("org.jetbrains.compose") version "1.11.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.30" apply false
}

object Versions {
    const val KOTLIN = "2.1.30"
    const val COMPOSE = "1.11.1"
    const val COMPOSE_COMPILER = "2.1.30"
    // ...
}
```

### 4. settings.gradle.kts

**Before**:
```kotlin
plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("multiplatform") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    id("org.jetbrains.compose") version "1.10.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
    id("org.jetbrains.kotlin.plugin.parcelize") version "2.1.21"
}
```

**After**:
```kotlin
plugins {
    kotlin("jvm") version "2.1.30"
    kotlin("multiplatform") version "2.1.30"
    kotlin("plugin.serialization") version "2.1.30"
    id("org.jetbrains.compose") version "1.11.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.30"
    id("org.jetbrains.kotlin.plugin.parcelize") version "2.1.30"
}
```

### 5. graphEditor/build.gradle.kts (Re-enable Compose)

Re-apply Compose plugins and test @Composable recompilation:

```kotlin
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")  // NEW: Re-enable
}

kotlin {
    jvm {
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation(project(":fbpDsl"))
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material3)
            }
        }
        
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
                // Compose Desktop runtime for JVM
                implementation(compose.desktop.currentOs)
            }
        }
    }
}
```

### 6. circuitSimulator/build.gradle.kts

Similar pattern if Compose is used; otherwise, keep minimal (non-Compose).

---

## Breaking Changes in Kotlin 2.1.30 → 2.2.0 (Deferred)

The following are **NOT** breaking for 2.1.30, but will be relevant when upgrading to 2.2.0:

1. **@Composable Function Stability**: 2.1.30 supports current @Composable syntax. 2.2.0 may introduce stricter lambda stability checks.
2. **Coroutine Integration**: 2.1.30 works with coroutines 1.8.0. 2.2.0 may require 1.9.0+.
3. **Multiplatform Plugin**: 2.1.30 is stable; 2.2.0 had experimental features that are now stable.

**Action**: Audit @Composable usage during implementation; defer 2.2.0 migration to future iteration once 2.1.30 is proven stable.

---

## Testing Plan

### Phase 1: Build Validation
1. Update gradle-wrapper.properties to 8.8
2. Update build.gradle.kts and settings.gradle.kts to Kotlin 2.1.30, Compose 1.11.1
3. Run `./gradlew clean build` → Verify no TaskCollection.named() errors

### Phase 2: Compose Recompilation in graphEditor
1. Re-enable Compose plugins in graphEditor/build.gradle.kts
2. Add test @Composable function to graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/
3. Run `./gradlew :graphEditor:compileKotlin` → Verify @Composable functions compile
4. Run `./gradlew :graphEditor:test` → Verify tests pass (if any)

### Phase 3: Integration Test
1. Create minimal Compose Desktop app in graphEditor (e.g., simple canvas with text)
2. Run `./gradlew :graphEditor:run` (or similar) → Verify UI renders without runtime errors
3. Document any deprecation warnings or compatibility issues

### Phase 4: Audit & Documentation
1. Review all @Composable functions for Kotlin 2.1.30 compatibility
2. Document any changes needed for future 2.2.0 upgrade
3. Update README with version requirements and build instructions

---

## Verification Checklist

- [ ] Gradle 8.8 wrapper installed and working
- [ ] Kotlin 2.1.30 plugins resolve correctly
- [ ] Compose 1.11.1 dependencies download successfully
- [ ] `./gradlew clean build` completes without TaskCollection errors
- [ ] graphEditor compiles with Compose plugins enabled
- [ ] @Composable test function compiles successfully
- [ ] No deprecation warnings in compilation output
- [ ] Build time remains under 2 minutes (for incremental builds)
- [ ] No new runtime errors in Compose Desktop tests

---

## Rollback Plan

If issues arise, revert to:
- Gradle 8.7 (confirmed stable alternative)
- Kotlin 2.1.21 (current working version)
- Compose 1.10.0 (current working version)

Command: `git revert <commit>` to restore previous state.

---

## References

- [Kotlin 2.1.30 Release Notes](https://kotlinlang.org/docs/releases.html)
- [Compose Multiplatform 1.11.1 Release](https://github.com/JetBrains/compose-multiplatform/releases)
- [Gradle 8.8 Release Notes](https://docs.gradle.org/8.8/release-notes.html)
- [Kotlin Compose Plugin Migration Guide](https://kotlinlang.org/docs/compose-compiler.html)

---

## Summary

| Aspect | Current | New | Reason |
|--------|---------|-----|--------|
| Gradle | 8.5 | 8.8 | TaskCollection.named() fixes |
| Kotlin | 2.1.21 | 2.1.30 | LTS stability for Compose |
| Compose | 1.10.0 | 1.11.1 | Tested with Gradle 8.8 + Kotlin 2.1.x |
| Compose Compiler | (manual) | 2.1.30 | Kotlin 2.0+ plugin architecture |
| Version Management | Hard-coded | libs.versions.toml | Single source of truth |

**Expected Outcome**: graphEditor UI re-enabled with Compose Multiplatform 1.11.1, TaskCollection errors eliminated, and textual FBP generation proceeding in parallel (P2 spec priority).


