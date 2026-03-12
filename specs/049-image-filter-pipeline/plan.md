# Implementation Plan: Image Filter Pipeline

**Branch**: `049-image-filter-pipeline` | **Date**: 2026-03-12 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/049-image-filter-pipeline/spec.md`

## Summary

Create a "Smart Filter Pipeline" showcasing FBP image processing: 6 custom nodes (ImagePicker source, GrayscaleTransformer, EdgeDetector, ColorOverlay, SepiaTransformer processors, ImageViewer sink) wired into a flowGraph with fan-out/fan-in branching. Uses Compose Multiplatform ImageBitmap for cross-platform image data, pure Kotlin pixel manipulation for filters, and the existing CodeNodeIO runtime/module architecture.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Desktop 1.7.3 (ImageBitmap, Canvas, Image composable), kotlinx-coroutines 1.8.0
**Storage**: N/A (in-memory image processing, no persistence)
**Testing**: Manual visual verification + unit tests for pixel transformations
**Target Platform**: JVM Desktop (Compose Desktop), with KMP Android target planned
**Project Type**: KMP module (follows StopWatch/UserProfiles module pattern)
**Performance Goals**: Process a standard image (1920x1080) through the full pipeline in < 5 seconds
**Constraints**: No external image processing libraries (OpenCV, etc.) — pure Kotlin pixel manipulation for portability
**Scale/Scope**: 6 custom node types, 1 FlowGraph, 1 module with processing logic + UI

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **Licensing**: No new dependencies. Compose UI (Apache 2.0) already included.
- [x] **Code Quality**: Each node is a single-responsibility processing function. Processing logic in dedicated files.
- [x] **TDD**: Pixel transformation functions (grayscale, edge detect, sepia) are pure functions testable with unit tests.
- [x] **UX Consistency**: Follows existing module pattern (ViewModel + Composable). ImageViewer uses standard Compose Image composable.
- [x] **Performance**: Image processing runs on background coroutine dispatchers. UI thread never blocked.
- [x] **Observability**: Processing metadata (timing) attached to packets and displayed in UI.

No gate violations.

## Project Structure

### Documentation (this feature)

```text
specs/049-image-filter-pipeline/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── checklists/
    └── requirements.md  # Spec quality checklist
```

### Source Code (repository root)

```text
EdgeArtFilter/
├── build.gradle.kts
├── src/commonMain/kotlin/io/codenode/edgeartfilter/
│   ├── EdgeArtFilter.flow.kt                    # FlowGraph definition (5 nodes, fan-out/fan-in)
│   ├── EdgeArtFilterViewModel.kt                # ViewModel with image state
│   ├── ImageData.kt                             # ImagePacket data class with metadata
│   ├── generated/
│   │   ├── EdgeArtFilterFlow.kt                 # Generated runtime wiring
│   │   ├── EdgeArtFilterController.kt           # Generated controller
│   │   ├── EdgeArtFilterControllerInterface.kt
│   │   └── EdgeArtFilterControllerAdapter.kt
│   ├── processingLogic/
│   │   ├── ImagePickerSourceLogic.kt            # File chooser → ImageData
│   │   ├── GrayscaleTransformerProcessLogic.kt  # Luminosity grayscale
│   │   ├── EdgeDetectorProcessLogic.kt          # Sobel edge detection
│   │   ├── ColorOverlayProcessLogic.kt          # Merge original + edges
│   │   ├── SepiaTransformerProcessLogic.kt      # Sepia tone filter
│   │   └── ImageViewerSinkLogic.kt              # Display result
│   └── userInterface/
│       └── EdgeArtFilter.kt                     # Compose UI with image display + metadata
```

**Structure Decision**: Standalone KMP module following the StopWatch/UserProfiles pattern. No persistence needed — purely in-memory image processing pipeline.

## Complexity Tracking

No constitution violations. No complexity justification needed.
