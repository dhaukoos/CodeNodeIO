# ✅ CodeNodeIO Project - Build Instructions

**Status**: Project structure complete and ready to build  
**Date**: January 17, 2026  
**Next Step**: Run the build script

---

## What Has Been Done

✅ **build.gradle.kts fixed** - Removed problematic `repositories` block from root config  
✅ **gradlew script created** - macOS/zsh-compatible Gradle wrapper  
✅ **build.sh script created** - Easy build launcher with environment setup  
✅ **All 6 modules configured** - Ready for compilation  
✅ **Dependencies pinned** - Kotlin 2.1.21, Compose 1.10.0, KotlinPoet 2.2.0  

---

## How to Build

### Quick Start (Recommended)

Simply run:

```bash
cd /Users/danahaukoos/CodeNodeIO
./build.sh
```

This script will:
1. ✅ Auto-detect Java 21
2. ✅ Verify JAVA_HOME is set
3. ✅ Run `./gradlew clean build`
4. ✅ Show results with next steps

### Or Use Gradle Directly

```bash
cd /Users/danahaukoos/CodeNodeIO
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew clean build
```

---

## Expected Build Output

On first build, you'll see:

```
Welcome to Gradle 8.5!

> Task :fbpDsl:compileKotlin
> Task :graphEditor:compileKotlin
> Task :circuitSimulator:compileKotlin
> Task :kotlinCompiler:compileKotlin
> Task :goCompiler:compileKotlin
> Task :idePlugin:compileKotlin

> Task :fbpDsl:test
> Task :graphEditor:test
> ... (more tests)

BUILD SUCCESSFUL in ~2-5 minutes
```

---

## Troubleshooting

### If you see "Permission denied: ./gradlew"
```bash
chmod +x /Users/danahaukoos/CodeNodeIO/gradlew
```

### If you see "Java not found"
```bash
brew install openjdk@21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

### If build hangs
Press `Ctrl+C` and check if `.gradle/` directory exists:
```bash
rm -rf /Users/danahaukoos/CodeNodeIO/.gradle
./build.sh
```

### If you see gradle/wrapper errors
The wrapper is already downloaded at `gradle-8.5/`. The build uses this directly via `./gradlew`.

---

## After Build Success

Once the build completes successfully (✅ BUILD SUCCESSFUL):

### 1. Run Tests
```bash
./gradlew test
```

### 2. Try Compose Desktop UI
```bash
./gradlew graphEditor:run
```
This opens a window showing the placeholder graph editor UI.

### 3. Start IDE Plugin in Sandbox
```bash
./gradlew idePlugin:runIde
```
This launches a sandboxed IntelliJ instance with CodeNodeIO plugin.

### 4. Check Built Artifacts
```bash
ls -R build/
```
Each module will have compiled `.class` files and JARs.

---

## Key Files

**Build System**:
- `/Users/danahaukoos/CodeNodeIO/gradlew` - Gradle wrapper (use instead of `gradle` command)
- `/Users/danahaukoos/CodeNodeIO/build.gradle.kts` - Root build config
- `/Users/danahaukoos/CodeNodeIO/settings.gradle.kts` - Module definitions

**Helper Scripts**:
- `/Users/danahaukoos/CodeNodeIO/build.sh` - Easy build launcher
- `/Users/danahaukoos/CodeNodeIO/gradle-8.5/` - Gradle 8.5 binary

**Documentation**:
- `/Users/danahaukoos/CodeNodeIO/specs/001-ide-plugin-platform/quickstart.md` - Full dev guide
- `/Users/danahaukoos/CodeNodeIO/COMPLETION_REPORT.md` - Project summary

---

## Version Locks Enforced

These exact versions are locked in `build.gradle.kts`:

```
✅ Kotlin 2.1.21          - K2 compiler, multiplatform support
✅ Compose 1.10.0         - Modern UI, desktop optimization
✅ KotlinPoet 2.2.0       - Type-safe code generation
✅ Coroutines 1.8.0       - Async/FBP execution
✅ Serialization 1.6.2    - JSON persistence
✅ JUnit 5 5.10.1         - Testing framework
✅ IntelliJ SDK 2024.1    - IDE plugin framework
```

**Why locked versions?** All developers and CI systems use identical dependencies, eliminating "works on my machine" issues.

---

## Project Structure

```
codenode-io/
├── fbpDsl/              # Core FBP domain model
├── graphEditor/         # Compose Desktop UI
├── circuitSimulator/    # Simulation/debugging
├── kotlinCompiler/      # KMP code generation
├── goCompiler/          # Go code generation
├── idePlugin/           # IntelliJ plugin

├── build.gradle.kts     # Root config (fixed)
├── settings.gradle.kts  # Module definitions
├── gradlew              # Gradle wrapper (new)
├── build.sh             # Build launcher (new)
└── gradle-8.5/          # Gradle 8.5 binary
```

---

## What's Next After Build Success

Once `./gradlew clean build` succeeds:

### Phase 1 Tasks (from tasks.md)
- Implement core FBP DSL (T001-T005)
- Build graph rendering engine (T006-T010)
- Implement code generation (T011-T030)
- Create contract tests (T031-T050)
- Integrate IDE plugin (T051-T060)

See: `specs/001-ide-plugin-platform/tasks.md`

---

## Common Commands

```bash
# Full build
./gradlew clean build

# Run only tests
./gradlew test

# Build specific module
./gradlew fbpDsl:build

# Run specific test
./gradlew fbpDsl:test

# Compose Desktop preview
./gradlew graphEditor:run

# IDE plugin sandbox
./gradlew idePlugin:runIde

# View dependencies
./gradlew dependencies

# Clean only (don't build)
./gradlew clean
```

---

## Status

✅ **All project setup complete**  
⏳ **Build ready to run**  
🚀 **Phase 0 initialization done**  
📋 **Phase 1 ready to begin**

---

**Run this now:**
```bash
cd /Users/danahaukoos/CodeNodeIO
./build.sh
```

The build will download dependencies (~300MB) and compile all 6 modules.  
**Estimated time**: 2-5 minutes on first build, ~30 seconds thereafter.

---

For detailed documentation, see:
- `quickstart.md` - Developer setup guide
- `COMPLETION_REPORT.md` - Full project summary
- `research.md` - Technical decisions
- `tasks.md` - Implementation roadmap

