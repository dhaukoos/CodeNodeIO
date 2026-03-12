# Quickstart: Image Filter Pipeline

**Feature**: 049-image-filter-pipeline | **Date**: 2026-03-12

## Overview

This feature creates a "Real-Time Edge-Art Filter" module with 6 custom node types demonstrating FBP image processing. An image flows from a source through grayscale conversion, edge detection, and color overlay, producing a "neon edge" composite.

## Implementation Steps

### Step 1: Create ImageData model

Define the data class that flows through the pipeline:

```kotlin
data class ImageData(
    val bitmap: ImageBitmap,
    val width: Int,
    val height: Int,
    val metadata: Map<String, String> = emptyMap()
)
```

### Step 2: Create the FlowGraph definition

Define `EdgeArtFilter.flow.kt` with 5 nodes and fan-out/fan-in connections:
- ImagePicker (source, 0→1)
- GrayscaleTransformer (processor, 1→1)
- EdgeDetector (processor, 1→1)
- ColorOverlay (processor, 2→1)
- ImageViewer (sink, 1→0)

### Step 3: Implement processing logic

Each node gets a processing logic file:
- **ImagePicker**: File chooser → load image → emit ImageData
- **GrayscaleTransformer**: Luminosity formula per pixel
- **EdgeDetector**: Sobel 3x3 convolution + optional delay
- **ColorOverlay**: Merge original + edges with neon color
- **SepiaTransformer**: Sepia matrix transformation (swap-in alternative)
- **ImageViewer**: Update ViewModel state for UI rendering

### Step 4: Generate runtime files

Use CodeNodeIO's "Save Module" to generate:
- EdgeArtFilterFlow.kt (runtime wiring)
- EdgeArtFilterController.kt (lifecycle management)
- EdgeArtFilterControllerInterface.kt
- EdgeArtFilterControllerAdapter.kt

### Step 5: Create ViewModel and UI

- ViewModel exposes `StateFlow<ImageData?>` for the processed image
- UI composable renders the image with metadata overlay
- File picker button triggers the pipeline

### Step 6: Register module in graphEditor

- Create custom node definitions for all 6 nodes
- Register PreviewProvider for Runtime Preview panel
- Add ModuleSessionFactory case
- Wire Koin and Main.kt integrations

### Step 7: Verify

1. Open graphEditor, construct the 5-node pipeline
2. Connect: ImagePicker → GrayscaleTransformer → EdgeDetector, ImagePicker → ColorOverlay, EdgeDetector → ColorOverlay → ImageViewer
3. Start execution, select an image
4. Verify: neon edge composite appears in ImageViewer with timing metadata
5. Swap GrayscaleTransformer with SepiaTransformer, re-run, verify different effect

## Key Files

| File | Change |
|------|--------|
| `EdgeArtFilter/src/.../ImageData.kt` | New: ImagePacket data class |
| `EdgeArtFilter/src/.../EdgeArtFilter.flow.kt` | New: FlowGraph definition |
| `EdgeArtFilter/src/.../processingLogic/*.kt` | New: 6 processing logic files |
| `EdgeArtFilter/src/.../EdgeArtFilterViewModel.kt` | New: ViewModel |
| `EdgeArtFilter/src/.../userInterface/EdgeArtFilter.kt` | New: UI composable |
