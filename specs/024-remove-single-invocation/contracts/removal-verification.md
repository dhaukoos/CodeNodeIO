# Contract: Removal Verification

**Feature**: 024-remove-single-invocation
**Date**: 2026-02-20

## Verification Protocol

Since this is a deletion feature, the "contract" is the verification protocol that ensures each removal step is safe.

### Per-Method Removal Verification

For each factory method removed from CodeNodeFactory:

1. **Pre-check**: Search entire codebase for method name — confirm zero callers outside the definition
2. **Remove**: Delete the method from CodeNodeFactory.kt
3. **Compile**: `./gradlew :fbpDsl:compileKotlinJvm` must succeed
4. **Test**: `./gradlew :fbpDsl:jvmTest` must pass (after removing associated tests)

### Per-File Removal Verification

For each file deleted:

1. **Pre-check**: Search entire codebase for all public symbols exported by the file — confirm zero imports
2. **Remove**: Delete the file
3. **Compile**: `./gradlew :fbpDsl:compileKotlinJvm` must succeed
4. **Cross-module**: `./gradlew :graphEditor:compileKotlinJvm :kotlinCompiler:compileKotlinJvm :StopWatch:compileKotlinJvm` must succeed
5. **Test**: `./gradlew test` all module tests pass

### Final Verification

After all removals are complete:

1. **Full build**: `./gradlew build` succeeds across all modules
2. **No dangling references**: Search for all removed symbol names returns zero results in source code
3. **Documentation cleanup**: No remaining references to removed patterns in docs or specs (except historical specs which reference patterns as past decisions)
