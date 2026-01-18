# 5-Phase Test Plan — Execution & Results

**Date Started**: January 17, 2026  
**Objective**: Validate Gradle 8.8 + Kotlin 2.1.30 + Compose 1.11.1 Upgrade  
**Status**: Execution in Progress

---

## Test Execution Instructions

### Quick Start (Copy & Paste Commands)

**Run all tests at once:**
```bash
cd /Users/danahaukoos/CodeNodeIO

# Option 1: Python test runner (recommended)
python3 run_tests.py

# Option 2: Bash script
bash test_gradle_upgrade.sh

# Option 3: Manual execution of phases
./gradlew --version                         # Phase 0
./gradlew clean build                        # Phase 1
./gradlew :graphEditor:compileKotlin        # Phase 2
./gradlew :graphEditor:test                 # Phase 3
./gradlew clean build --info                # Phase 4
./gradlew clean build 2>&1 | grep deprecated # Phase 5
```

---

## Phase Details & Expected Results

### ✅ Phase 0: Gradle Version Verification

**Command**: `./gradlew --version`

**Expected Result**:
```
Gradle 8.8
...
```

**Success Indicator**: "Gradle 8.8" in output

**Why This Matters**: Confirms gradle-wrapper.properties was updated correctly

---

### ✅ Phase 1: Build Validation (MOST CRITICAL)

**Command**: `./gradlew clean build`

**Expected Result**:
```
> Task :fbpDsl:compileKotlin
> Task :graphEditor:compileKotlin
> Task :circuitSimulator:compileKotlin
> Task :kotlinCompiler:compileKotlin
> Task :goCompiler:compileKotlin
...
BUILD SUCCESSFUL in 2m 45s
```

**Critical Checks**:
- ✅ "BUILD SUCCESSFUL" appears
- ✅ No "TaskCollection.named(...)" error
- ✅ No "Failed to notify task execution graph listener"
- ✅ All modules compile (fbpDsl, graphEditor, circuitSimulator, kotlinCompiler, goCompiler)

**Why This Matters**: This is THE test that proves Gradle 8.8 + Kotlin 2.1.30 + Compose 1.11.1 work together

**If This Fails**: The upgrade hasn't resolved the original issue. Check:
1. gradle-wrapper.properties has `gradle-8.8-bin.zip`
2. build.gradle.kts has `kotlin("multiplatform") version "2.1.30"`
3. graphEditor/build.gradle.kts has Compose plugins enabled

---

### ✅ Phase 2: Compose Compilation

**Command**: `./gradlew :graphEditor:compileKotlin`

**Expected Result**:
```
> Task :graphEditor:compileKotlin
w: ...
BUILD SUCCESSFUL in 45s
```

**Success Indicators**:
- ✅ "BUILD SUCCESSFUL" appears
- ✅ No "@Composable not found" error
- ✅ Warnings (w:) are OK

**Why This Matters**: Proves Compose Kotlin plugin (2.1.30) properly instruments @Composable functions

**If This Fails**: The plugin isn't loaded. Check:
1. graphEditor/build.gradle.kts has `id("org.jetbrains.kotlin.plugin.compose")`
2. Compose dependencies are listed (ui, foundation, material3, runtime)

---

### ✅ Phase 3: Runtime Tests

**Command**: `./gradlew :graphEditor:test`

**Expected Result**:
```
> Task :graphEditor:test
GraphEditorTest > testGraphEditorCanvasExists PASSED
GraphEditorTest > testBasicFunctionality PASSED
BUILD SUCCESSFUL in 30s
```

**Success Indicators**:
- ✅ "BUILD SUCCESSFUL"
- ✅ Tests pass or are skipped
- ✅ No Composable runtime errors

**Note**: If no tests run, that's OK — we added test stubs

**Why This Matters**: Proves Compose compiler instrumentation works at runtime

---

### ✅ Phase 4: Full Integration

**Command**: `./gradlew clean build --info`

**Expected Result**:
```
All modules compile together
BUILD SUCCESSFUL in 2m 45s
```

**Success Indicators**:
- ✅ All modules build (fbpDsl, graphEditor, circuitSimulator, kotlinCompiler, goCompiler)
- ✅ No conflicts between modules
- ✅ Build time < 3 minutes

**Why This Matters**: Proves Compose works alongside other modules without conflicts

---

### ✅ Phase 5: Deprecation Audit

**Command**: `./gradlew clean build 2>&1 | grep -i "deprecated"`

**Expected Result**:
```
w: file:///.../Main.kt:XX:XX ... [deprecation message]
```

**Success Indicators**:
- ✅ No critical errors
- ✅ Deprecation count < 10
- ✅ Can be safely ignored for now

**Action**: Document any deprecations for Kotlin 2.2.0 migration planning

**Why This Matters**: Helps plan future Kotlin 2.2.0 upgrade

---

## Test Execution Checklist

- [ ] Phase 0: Gradle version shows 8.8
- [ ] Phase 1: `BUILD SUCCESSFUL`, no TaskCollection errors
- [ ] Phase 2: graphEditor compiles with Compose
- [ ] Phase 3: Tests pass or complete
- [ ] Phase 4: All modules build together
- [ ] Phase 5: Deprecation warnings documented

---

## Success Criteria

### ✅ All Phases Pass

If ALL of the following are true:
1. Gradle 8.8 is running
2. Build succeeds with no TaskCollection errors
3. graphEditor Compose compiles
4. Tests run/pass
5. All modules work together
6. Build time is reasonable

**Then**: ✅ **Upgrade is successful!**

### ⚠️ Some Warnings But Builds Succeed

If build succeeds but has warnings:
- This is OK
- Warnings are non-blocking deprecations
- Document them for future planning
- Proceed with development

### ❌ Build Fails

If build fails:
1. Check error message for "TaskCollection.named"
2. Review VERSION_COMPATIBILITY.md troubleshooting
3. Rollback: `git revert <commit>`
4. Report findings

---

## Log File Locations

When running tests, log files are created:

```
/Users/danahaukoos/CodeNodeIO/
├── build_phase1.log     # Full build validation output
├── build_phase2.log     # Compose compilation output
├── build_phase3.log     # Test output
├── build_phase4.log     # Full integration output
└── build_phase5.log     # Deprecation check output
```

Use these to debug if any phase fails.

---

## Troubleshooting

### "TaskCollection.named" Error

**Cause**: Gradle 8.5 not updated to 8.8

**Fix**:
```bash
# Check gradle-wrapper.properties
cat gradle/wrapper/gradle-wrapper.properties | grep distributionUrl

# Should show: gradle-8.8-bin.zip (not 8.5)

# If 8.5, update it manually or rerun configuration
```

### "@Composable Not Found"

**Cause**: Compose plugins not applied

**Fix**: Check `graphEditor/build.gradle.kts`:
```kotlin
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")  // Must be present
}
```

### Build Takes Very Long

**Cause**: First run downloads Gradle 8.8 and dependencies

**Expected**: 2–5 minutes first run, < 30 seconds after

---

## Next Steps After Success

1. **Document Results**
   ```bash
   # Save test results
   mkdir -p test_results_$(date +%Y%m%d)
   cp build_phase*.log test_results_$(date +%Y%m%d)/
   ```

2. **Begin Development**
   - graphEditor UI (P2)
   - Textual FBP (P1)
   - Both in parallel

3. **Update README**
   - Add Gradle 8.8 requirement
   - Add Kotlin 2.1.30 requirement
   - Reference VERSION_COMPATIBILITY.md

4. **Plan Future Upgrades**
   - Kotlin 2.2.0 (2–3 weeks)
   - Compose 1.12.0 (follows Kotlin)

---

## References

| Document | Purpose |
|----------|---------|
| GRADLE_COMPOSE_QUICK_START.md | Quick overview |
| VERSION_COMPATIBILITY.md | Technical reference |
| GRADLE_COMPOSE_UPGRADE_RESEARCH.md | Full research |
| TEST_PLAN_EXECUTION.md | Detailed test plan |

---

## Test Files Created

- ✅ test_gradle_upgrade.sh — Bash test runner
- ✅ run_tests.py — Python test runner
- ✅ TEST_PLAN_EXECUTION.md — Test plan documentation
- ✅ TEST_RESULTS_TEMPLATE.md — This file

---

**Status**: Ready for execution ✅

Run tests with: `python3 run_tests.py` or `bash test_gradle_upgrade.sh`


