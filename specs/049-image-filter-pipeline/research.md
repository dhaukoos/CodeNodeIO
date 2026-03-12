# Research: Image Filter Pipeline

**Feature**: 049-image-filter-pipeline | **Date**: 2026-03-12

## Research Tasks

### 1. Image data type for cross-platform KMP

**Decision**: Use `ImageBitmap` from Compose Multiplatform wrapped in a custom `ImageData` data class that carries metadata.

**Findings**:
- `androidx.compose.ui.graphics.ImageBitmap` is available in all Compose Multiplatform targets (JVM, Android, iOS)
- Supports `readPixels()` for pixel-level access and `toPixelMap()` for reading
- On JVM/Desktop, backed by `java.awt.image.BufferedImage` which supports direct pixel manipulation
- `ImageData(bitmap: ImageBitmap, metadata: Map<String, String>)` wraps both image and processing info

**Alternatives considered**:
- Raw `IntArray` of pixels — too low-level, loses dimension info
- `ByteArray` with format — requires encoding/decoding overhead
- `BufferedImage` (JVM only) — not cross-platform

### 2. Image processing approach without external libraries

**Decision**: Use platform-specific pixel manipulation via `expect/actual` pattern. On JVM, use `BufferedImage` for efficient pixel access.

**Findings**:
- `ImageBitmap.readPixels()` is available but performance varies by platform
- On JVM, convert to `BufferedImage` via `toAwtImage()` for fast pixel array access
- Grayscale: `0.299R + 0.587G + 0.114B` luminosity formula
- Sobel edge detection: 3x3 convolution kernels for horizontal/vertical gradients
- Sepia: Apply matrix transformation `[0.393R+0.769G+0.189B, 0.349R+0.686G+0.168B, 0.272R+0.534G+0.131B]`
- Color overlay: Alpha blend original with edge map using additive compositing

**Alternatives considered**:
- OpenCV KMP bindings — heavy dependency, licensing concerns, not needed for basic filters
- Korge/KorIM — additional dependency for limited benefit
- Compose Canvas drawing — suitable for rendering but not pixel manipulation

### 3. Fan-out pattern for ImagePicker → two targets

**Decision**: Use the existing Connection model — create two separate connections from ImagePicker's output port.

**Findings**:
- CodeNodeIO's `Connection` model already supports fan-out natively
- One output port can have multiple connections to different input ports
- At runtime, channel wiring creates separate channels for each connection
- The same data packet is sent to both downstream nodes independently
- No special fan-out node or splitter needed

### 4. ColorOverlay as a 2-input merger node

**Decision**: Use `In2Out1Runtime<ImageData, ImageData, ImageData>` — two image inputs, one merged output.

**Findings**:
- `In2Out1Runtime` receives from both input channels synchronously via `select {}`
- Both inputs must arrive before processing produces output
- Perfect for merging original image + edge-detected image
- The `In2Out1ProcessBlock<A, B, R>` type alias defines the process function signature

### 5. Module structure and code generation

**Decision**: Create the module manually following StopWatch/UserProfiles pattern. Use the graph editor's "Save Module" to generate runtime files.

**Findings**:
- Modules follow consistent structure: `.flow.kt` → generated runtime files → processing logic → ViewModel → UI
- The `.flow.kt` file defines the FlowGraph using the Kotlin DSL
- `ModuleSaveService.saveModule()` generates runtime files from the FlowGraph
- Processing logic stubs are generated but need manual implementation for image algorithms
- ViewModel manages state exposure to the UI composable

### 6. Processing metadata propagation

**Decision**: Include metadata in the `ImageData` wrapper class. Each node adds its processing time before forwarding.

**Findings**:
- The existing `InformationPacket` model supports `metadata: Map<String, String>`
- Simpler approach: put metadata directly in the `ImageData` data class
- Each processing node measures `System.currentTimeMillis()` before and after, adds to metadata map
- ImageViewer sink reads metadata and displays timing alongside the image
- Metadata accumulates as the packet flows through nodes
