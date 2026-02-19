# Implementation Plan: Refactor Base NodeRuntime Class

**Branch**: `021-refactor-noderuntime-base` | **Date**: 2026-02-19 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/021-refactor-noderuntime-base/spec.md`

## Summary

Refactor the base `NodeRuntime` class to remove its generic type parameter `<T: Any>` and its `inputChannel`/`outputChannel` properties. Then update all 16 runtime subclasses to define their own typed channel properties. Single-input runtimes keep the name `inputChannel`; multi-input runtimes use numbered `inputChannel1`/`inputChannel2`/`inputChannel3`. Single-output runtimes use `outputChannel` (renaming prefixed names like `transformerOutputChannel` and `processorOutputChannel`); multi-output runtimes keep numbered `outputChannel1`/`outputChannel2`/`outputChannel3`. This separates lifecycle management (base class) from typed data flow (subclasses).

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: kotlinx-coroutines 1.8.0 (channels, coroutine scopes)
**Storage**: N/A (in-memory runtime state)
**Testing**: kotlin.test, kotlinx-coroutines-test (runTest, advanceTimeBy, virtual time)
**Target Platform**: JVM (primary), with KMP common source
**Project Type**: Multi-module KMP library (fbpDsl module + StopWatch module)
**Performance Goals**: N/A (pure refactoring, no behavior change)
**Constraints**: Zero behavior change - all existing tests must pass with only property name updates
**Scale/Scope**: 1 base class + 16 subclasses + 1 registry + 1 factory + ~8 test files + 2 StopWatch components

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | PASS | Refactoring improves maintainability by eliminating unused inherited properties and inconsistent naming |
| II. TDD | PASS | Existing tests serve as regression suite. Test updates are mechanical (property name changes only) |
| III. UX Consistency | N/A | Internal framework change, no user-facing impact |
| IV. Performance | PASS | Zero runtime behavior change, pure compile-time refactoring |
| V. Observability | N/A | No logging/metrics changes needed |

**Quality Gates**:
- All existing tests must pass after refactoring (automated tests gate)
- Consistent naming convention established (code quality gate)

## Project Structure

### Documentation (this feature)

```text
specs/021-refactor-noderuntime-base/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output - class hierarchy mapping
├── quickstart.md        # Phase 1 output - before/after examples
├── contracts/           # Phase 1 output - channel naming contracts
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/
├── runtime/
│   ├── NodeRuntime.kt              # Base class - MODIFY (remove generic + channels)
│   ├── GeneratorRuntime.kt         # MODIFY (own outputChannel, inherit non-generic)
│   ├── SinkRuntime.kt              # MODIFY (own inputChannel, inherit non-generic)
│   ├── TransformerRuntime.kt       # MODIFY (own inputChannel, rename transformerOutputChannel → outputChannel, inherit non-generic)
│   ├── FilterRuntime.kt            # MODIFY (own inputChannel + outputChannel, inherit non-generic)
│   ├── Out2GeneratorRuntime.kt     # MODIFY (inherit non-generic)
│   ├── Out3GeneratorRuntime.kt     # MODIFY (inherit non-generic)
│   ├── In2SinkRuntime.kt           # MODIFY (add inputChannel1, inherit non-generic)
│   ├── In3SinkRuntime.kt           # MODIFY (add inputChannel1, inherit non-generic)
│   ├── In1Out2Runtime.kt           # MODIFY (own inputChannel, inherit non-generic)
│   ├── In1Out3Runtime.kt           # MODIFY (own inputChannel, inherit non-generic)
│   ├── In2Out1Runtime.kt           # MODIFY (add inputChannel1, rename processorOutputChannel → outputChannel, inherit non-generic)
│   ├── In2Out2Runtime.kt           # MODIFY (add inputChannel1, inherit non-generic)
│   ├── In2Out3Runtime.kt           # MODIFY (add inputChannel1, inherit non-generic)
│   ├── In3Out1Runtime.kt           # MODIFY (add inputChannel1, rename processorOutputChannel → outputChannel, inherit non-generic)
│   ├── In3Out2Runtime.kt           # MODIFY (add inputChannel1, inherit non-generic)
│   ├── In3Out3Runtime.kt           # MODIFY (add inputChannel1, inherit non-generic)
│   └── RuntimeRegistry.kt         # MODIFY (NodeRuntime<*> → NodeRuntime)
│
├── model/
│   └── CodeNodeFactory.kt          # MODIFY (remove <T> from NodeRuntime references)

fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/runtime/
├── NodeRuntimeTest.kt              # MODIFY (update channel tests)
├── ContinuousFactoryTest.kt        # MODIFY (multi-input: inputChannel → inputChannel1, transformerOutputChannel → outputChannel)
├── TypedNodeRuntimeTest.kt         # MODIFY (multi-input: inputChannel → inputChannel1, processorOutputChannel → outputChannel)
├── PauseResumeTest.kt              # MODIFY (multi-input: inputChannel → inputChannel1, transformerOutputChannel → outputChannel)
├── IndependentControlTest.kt       # MODIFY (multi-input: inputChannel → inputChannel1)
├── RuntimeRegistrationTest.kt      # MODIFY (NodeRuntime<*> → NodeRuntime)
├── RuntimeRegistryTest.kt          # MODIFY (NodeRuntime<*> → NodeRuntime)
└── TimedGeneratorTest.kt           # No changes expected

StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/
├── DisplayReceiverComponent.kt     # MODIFY (no input rename - single-input keeps inputChannel)

StopWatch/src/commonTest/kotlin/io/codenode/stopwatch/
├── ChannelIntegrationTest.kt       # MODIFY (no input rename - single-input keeps inputChannel)
```

**Structure Decision**: This is a modification to the existing fbpDsl module structure. No new files or directories are created. All changes are modifications to existing files.

## Complexity Tracking

No constitution violations requiring justification.
