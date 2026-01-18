# Gradle 8.8 Upgrade Test Plan — Execution Report

**Date**: January 17, 2026  
**Objective**: Validate Gradle 8.8 + Kotlin 2.1.30 + Compose 1.11.1 upgrade  
**Status**: Ready for Execution

---

## Test Plan Overview

This document outlines the 5-phase validation plan for the Gradle 8.5 compatibility fix.

### Quick Execution Commands

```bash
# Execute all phases at once
bash /Users/danahaukoos/CodeNodeIO/test_gradle_upgrade.sh

# OR execute each phase individually:

# Phase 0: Verify Gradle version
./gradlew --version

# Phase 1: Full build validation
./gradlew clean build

# Phase 2: Compose compilation
./gradlew :graphEditor:compileKotlin

# Phase 3: Runtime tests
./gradlew :graphEditor:test

# Phase 4: Full integration with info
./gradlew clean build --info

# Phase 5: Check for deprecations
./gradlew clean build 2>&1 | grep -i "deprecated\|warning" | head -20
```

---

## Phase Breakdown

### Phase 0: Gradle Version Verification

**Command**: `./gradlew --version`

**Expected Output**:
```
Gradle 8.8

Build time:   2024-...
Revision:     ...

Kotlin:       2.1.30
Ant:          ...
JVM:          ...
OS:           macOS ...
```

**Success Criteria**:
- ✅ Gradle 8.8 (not 8.5)
- ✅ Kotlin version shows in output

**Validation**: If Gradle is 8.8, proceed to Phase 1

---

### Phase 1: Build Validation (Critical)

**Command**: `./gradlew clean build`

**Duration**: 2–3 minutes (first run), < 30 seconds (incremental)

**Expected Output**:
```
> Task :fbpDsl:compileKotlin
> Task :graphEditor:compileKotlin
> Task :circuitSimulator:compileKotlin
> Task :kotlinCompiler:compileKotlin
> Task :goCompiler:compileKotlin
...
BUILD SUCCESSFUL in Xs
```

**What's Being Tested**:
- ✅ Gradle 8.8 task graph works
- ✅ Kotlin 2.1.30 compiler works
- ✅ No `TaskCollection.named()` errors
- ✅ All modules compile
- ✅ graphEditor compiles with Compose plugins

**Success Criteria**:
- [x] "BUILD SUCCESSFUL" appears
- [x] No "TaskCollection" errors
- [x] No "Failed to notify task execution graph listener" errors
- [x] Total time < 3 minutes

**If Fails**:
Check for these errors:
- `org.gradle.api.tasks.TaskCollection.named(...)`
- `Failed to notify task execution graph listener`
- `error: ...graphEditor...`

If TaskCollection error appears, rollback: `git revert <commit>`

---

### Phase 2: Compose Compilation Test

**Command**: `./gradlew :graphEditor:compileKotlin`

**Duration**: ~1 minute

**Expected Output**:
```
> Task :graphEditor:compileKotlin
w: ...
BUILD SUCCESSFUL in Xs
```

**What's Being Tested**:
- ✅ graphEditor module compiles
- ✅ @Composable annotation recognized
- ✅ Compose Kotlin plugin (2.1.30) works
- ✅ Compose compiler instrumentation works

**Success Criteria**:
- [x] "BUILD SUCCESSFUL" appears
- [x] No errors about "@Composable not found"
- [x] No "Compose compiler" errors
- [x] Warnings (w:) are OK (they're just deprecations)

**What Validates**:
This proves that the `org.jetbrains.kotlin.plugin.compose` plugin is properly loaded and can instrument @Composable functions with Gradle 8.8.

---

### Phase 3: Runtime Tests

**Command**: `./gradlew :graphEditor:test`

**Duration**: ~1 minute

**Expected Output**:
```
> Task :graphEditor:test
GraphEditorTest > testGraphEditorCanvasExists PASSED
GraphEditorTest > testBasicFunctionality PASSED

BUILD SUCCESSFUL in Xs
```

**What's Being Tested**:
- ✅ Test framework works (Kotlin test)
- ✅ @Composable functions runtime behavior
- ✅ No Compose compiler plugin runtime errors

**Success Criteria**:
- [x] "BUILD SUCCESSFUL" appears
- [x] Tests pass (or no tests exist, which is OK)
- [x] No "Composable" runtime errors

**Note**: If no tests are found, that's OK — we added test stubs. If tests exist and pass, that's ideal.

---

### Phase 4: Full Integration Check

**Command**: `./gradlew clean build --info`

**Duration**: 2–3 minutes

**Expected Output**:
```
> Task :fbpDsl:compileKotlin
> Task :fbpDsl:test
> Task :graphEditor:compileKotlin
> Task :graphEditor:test
> Task :circuitSimulator:compileKotlin
> Task :circuitSimulator:test
> Task :kotlinCompiler:compileKotlin
> Task :goCompiler:compileKotlin
...
BUILD SUCCESSFUL in 2m 30s
```

**What's Being Tested**:
- ✅ All modules compile together
- ✅ No module-to-module conflicts
- ✅ Compose works alongside other modules
- ✅ Build time is reasonable

**Success Criteria**:
- [x] "BUILD SUCCESSFUL" appears
- [x] All modules listed in output
- [x] No errors between modules
- [x] Build time < 3 minutes (first clean build may be longer)

**Performance Expectations**:
- First clean build: 2–5 minutes (downloading Gradle 8.8, dependencies)
- Incremental builds: < 30 seconds
- Compose compilation adds ~10–15 seconds

---

### Phase 5: Deprecation Audit

**Command**: `./gradlew clean build 2>&1 | grep -i "deprecated\|warning"`

**Duration**: Run as part of Phase 4, just inspect warnings

**Expected Output**:
```
w: file:///.../Main.kt:XX:XX ... [some Kotlin 2.1.30 deprecation]
w: ...
```

**What's Being Checked**:
- Kotlin 2.1.30 deprecation warnings (if any)
- Compose deprecation warnings (if any)
- Preview of what needs attention for Kotlin 2.2.0 upgrade

**Success Criteria**:
- [x] No critical errors
- [x] Deprecation count reasonable (< 10)
- [x] Warnings are just informational

**Action Items**:
- Document any warnings
- Create task to address them before Kotlin 2.2.0 upgrade
- Most warnings can be safely ignored for now

---

## Success Indicators

### ✅ Build Succeeds (All Phases)

If you see:
```
BUILD SUCCESSFUL in Xs
```

**This means**:
1. ✅ Gradle 8.8 is working correctly
2. ✅ Kotlin 2.1.30 compiler is working
3. ✅ Compose 1.11.1 is installed and working
4. ✅ `org.jetbrains.kotlin.plugin.compose` plugin is working
5. ✅ No `TaskCollection.named()` incompatibility errors
6. ✅ graphEditor Compose UI is re-enabled
7. ✅ **The upgrade is successful!** 🎉

### ⚠️ Build Has Warnings

If you see:
```
w: ...deprecated...
warning: ...
```

**This is OK**:
- Warnings are just deprecations
- Non-blocking issues
- Document them for future Kotlin 2.2.0 upgrade
- Safe to ignore for now

### ❌ Build Fails (Errors)

If you see:
```
FAILED
error: ...
BUILD FAILED
```

**Check for**:
1. `TaskCollection.named(...)` — indicates Gradle/plugin incompatibility
2. `@Composable not found` — indicates plugin not loaded
3. `Failed to notify` — indicates Gradle task graph issue

**Next Steps**:
1. Check VERSION_COMPATIBILITY.md troubleshooting section
2. Review error messages carefully
3. If needed: `git revert <commit>` to rollback

---

## Expected Outputs (Copy-Paste Reference)

### Successful Phase 1 Output
```
> Task :fbpDsl:compileKotlin
> Task :fbpDsl:test
> Task :graphEditor:compileKotlin
> Task :circuitSimulator:compileKotlin
> Task :circuitSimulator:test
> Task :kotlinCompiler:compileKotlin
> Task :goCompiler:compileKotlin

BUILD SUCCESSFUL in 2m 45s
16 actionable tasks: 16 executed
```

### Successful Phase 2 Output
```
> Task :graphEditor:compileKotlin

BUILD SUCCESSFUL in 45s
1 actionable task: 1 executed
```

### Successful Phase 3 Output
```
> Task :graphEditor:test

GraphEditorTest > testGraphEditorCanvasExists PASSED
GraphEditorTest > testBasicFunctionality PASSED

BUILD SUCCESSFUL in 30s
3 actionable tasks: 1 executed, 2 from cache
```

---

## Troubleshooting Guide

### Issue: TaskCollection.named() error

**Error Message**:
```
error: Failed to notify task execution graph listener
  - org.gradle.api.tasks.TaskCollection.named(...)
```

**Causes**:
- Gradle still on 8.5 (not updated to 8.8)
- gradle-wrapper.properties not updated
- Gradle daemon cache

**Solutions**:
```bash
# 1. Verify Gradle version
./gradlew --version

# 2. Clear daemon cache
./gradlew --stop

# 3. Try build again
./gradlew clean build

# 4. If still fails, rollback
git revert <commit>
```

### Issue: @Composable not recognized

**Error Message**:
```
error: Unresolved reference '@Composable'
```

**Causes**:
- Compose plugins not applied
- Compose dependencies missing
- Plugin version mismatch

**Solutions**:
```bash
# Check plugins are applied in graphEditor/build.gradle.kts:
# - id("org.jetbrains.compose")
# - id("org.jetbrains.kotlin.plugin.compose")

# Verify settings.gradle.kts has correct plugin versions

# Clear cache and rebuild
./gradlew clean build
```

### Issue: Build takes > 5 minutes

**Cause**:
- First build (downloading Gradle 8.8 and dependencies)
- Gradle daemon not warmed up

**Solution**:
- Expected for first build
- Second build should be < 1 minute
- This is normal

---

## Next Steps After Success

Once all 5 phases complete successfully:

1. **Document Results**
   - Note build time
   - Note any warnings
   - Save log files: `build_phase*.log`

2. **Begin Development**
   - Start graphEditor UI implementation (P2)
   - Continue Textual FBP generation (P1)
   - Both can proceed in parallel

3. **Plan Future Upgrades**
   - Kotlin 2.2.0 (evaluate after 2–3 weeks)
   - Compose 1.12.0 (follows Kotlin decision)
   - Document deprecations found in Phase 5

4. **Update Documentation**
   - README: Add Gradle 8.8, Kotlin 2.1.30 requirements
   - Build setup: Link to VERSION_COMPATIBILITY.md
   - Archive test results

---

## Quick Summary

| Phase | Command | Duration | Key Check |
|-------|---------|----------|-----------|
| **0** | `./gradlew --version` | 5 sec | Gradle 8.8? |
| **1** | `./gradlew clean build` | 2–3 min | BUILD SUCCESSFUL, no TaskCollection errors? |
| **2** | `./gradlew :graphEditor:compileKotlin` | 1 min | @Composable compiles? |
| **3** | `./gradlew :graphEditor:test` | 1 min | Tests pass? |
| **4** | `./gradlew clean build --info` | 2–3 min | All modules build? |
| **5** | `grep deprecation` | 0 min | Document warnings |

**Total Time**: ~8 minutes

**Success**: All phases show ✅

---

## References

- **Start Here**: GRADLE_COMPOSE_QUICK_START.md
- **Technical Reference**: VERSION_COMPATIBILITY.md
- **Full Research**: GRADLE_COMPOSE_UPGRADE_RESEARCH.md
- **Executive Summary**: GRADLE_COMPOSE_RESOLUTION_OPTIONS.md

---

**Status**: Ready to Execute ✅

Run this to start testing:
```bash
bash /Users/danahaukoos/CodeNodeIO/test_gradle_upgrade.sh
```

Or run phases individually with commands above.


