# Research: Refactor Base NodeRuntime Class

**Feature**: 021-refactor-noderuntime-base
**Date**: 2026-02-19

## Decision 1: Input Channel Naming Convention

**Decision**: Single-input runtimes keep the name `inputChannel` (no number suffix). Multi-input runtimes use numbered names: `inputChannel1`, `inputChannel2`, `inputChannel3`.

**Rationale**: A single-input runtime has only one input, so numbering is unnecessary and adds verbosity. Multi-input runtimes already use `inputChannel2` and `inputChannel3`, so adding `inputChannel1` completes the numbered scheme. This mirrors the output channel convention where single-output uses `outputChannel` and multi-output uses numbered names.

**Alternatives Considered**:
- Rename all to `inputChannel1` even on single-input subclasses (over-specification, inconsistent with output naming)
- Use `primaryInputChannel` (non-standard, verbose)

## Decision 2: GeneratorRuntime Output Channel Name

**Decision**: Keep `outputChannel` (singular, no number) on `GeneratorRuntime` since it has only one output.

**Rationale**: Multi-output generators use `outputChannel1`, `outputChannel2`, `outputChannel3`. Single-output generators only have one channel, so the numbered suffix is unnecessary. The singular name is more natural.

**Alternatives Considered**:
- Rename to `outputChannel1` for consistency (over-specification for a single output)

## Decision 3: Base Class `start()` Cleanup

**Decision**: Remove the `finally { outputChannel?.close() }` block from `NodeRuntime.start()` since `outputChannel` will no longer exist on the base class.

**Rationale**: All subclasses that have output channels already handle their own channel closure in their overridden `start()` methods. The base class `finally` block is dead code for most subclasses.

**Research Finding - Subclasses that override start()**:
- GeneratorRuntime: Yes - closes outputChannel in its own `finally` block
- SinkRuntime: Yes - no output channel to close
- TransformerRuntime: Yes - closes outputChannel in its own `finally` block
- FilterRuntime: Yes - closes outputChannel in its own `finally` block
- Out2GeneratorRuntime: Yes - closes outputChannel1/outputChannel2 in its own `finally` block
- Out3GeneratorRuntime: Yes - closes all three output channels
- All In*Out* runtimes: Yes - each closes its own output channels
- All In*Sink runtimes: Yes - no output channels to close

**Conclusion**: All 16 subclasses override `start()` and manage their own channel lifecycle. The base class `finally` block is never relied upon.

## Decision 4: Scope of Channel Property Changes

**Decision**: The refactoring has two scopes: (1) single-input runtimes define their own `inputChannel` property (same name, now owned instead of inherited), (2) multi-input runtimes rename inherited `inputChannel` to `inputChannel1` (numbered scheme).

**Research Finding - Complete reference inventory**:

| Category | Change | Files |
|----------|--------|-------|
| Base class definition | Remove `inputChannel` + `outputChannel` | NodeRuntime.kt |
| Single-input runtimes | Add own `inputChannel` property (same name) | SinkRuntime, TransformerRuntime, FilterRuntime |
| Multi-input runtimes | Add own `inputChannel1` property (renamed) | All In*Out* and In*Sink classes (10 files) |
| Single-output runtimes | Rename prefixed output → `outputChannel` | TransformerRuntime, In2Out1Runtime, In3Out1Runtime |
| Test file references | Update multi-input refs only | ~5 test files |
| StopWatch component | No change (single-input, keeps `inputChannel`) | DisplayReceiverComponent.kt |
| StopWatch test | No change (single-input, keeps `inputChannel`) | ChannelIntegrationTest.kt |

**Key insight**: `inputChannel` is defined once on NodeRuntime.kt. After removing it from the base class, single-input subclasses add it as their own property (same name, no consumer changes needed), while multi-input subclasses add `inputChannel1` (consumers update references).

## Decision 5: RuntimeRegistry Wildcard Removal

**Decision**: Replace `NodeRuntime<*>` with plain `NodeRuntime` in RuntimeRegistry.

**Rationale**: Once NodeRuntime has no generic parameter, the wildcard star projection is no longer valid or needed. All `NodeRuntime<*>` references become simply `NodeRuntime`.

**Files affected**: RuntimeRegistry.kt, RuntimeRegistryTest.kt, RuntimeRegistrationTest.kt

## Decision 6: Output Channel Naming for Single-Output Runtimes

**Decision**: All single-output runtimes use `outputChannel` (no prefix, no number). This includes renaming `transformerOutputChannel` → `outputChannel` on TransformerRuntime and `processorOutputChannel` → `outputChannel` on In2Out1Runtime and In3Out1Runtime.

**Rationale**: The naming convention mirrors the input channel convention: single-channel uses the plain name, multi-channel uses numbered names. Having prefixed names like `transformerOutputChannel` was a workaround when the base class owned `outputChannel` and subclasses needed distinct names. With the base class no longer owning channel properties, the prefix is unnecessary.

**Alternatives Considered**:
- Keep `transformerOutputChannel` and `processorOutputChannel` (inconsistent with the "single = plain name" convention)
- Rename all to `outputChannel1` (over-specification for single output)

**Files affected**: TransformerRuntime.kt, In2Out1Runtime.kt, In3Out1Runtime.kt, and their corresponding test files
