# Node Contracts: Image Filter Pipeline

**Feature**: 049-image-filter-pipeline | **Date**: 2026-03-12

## ImagePicker Source

### Input
None (source node).

### Output
- Port "image": `ImageData` — loaded image with source metadata

### Behavior
- On trigger: opens platform file chooser dialog
- Loads selected image file into ImageBitmap
- Wraps in ImageData with `source` metadata key
- Emits on output port
- If user cancels dialog: no emission, no error

## GrayscaleTransformer Processor

### Input
- Port "image": `ImageData`

### Output
- Port "result": `ImageData` — grayscale version

### Algorithm
For each pixel: `gray = 0.299 * R + 0.587 * G + 0.114 * B`
Set R=G=B=gray. Alpha preserved. Adds `grayscale_ms` to metadata.

## EdgeDetector Processor

### Input
- Port "image": `ImageData` (expects grayscale)

### Output
- Port "edges": `ImageData` — edge map (bright edges on black)

### Algorithm
Sobel 3x3 convolution:
- Gx kernel: `[[-1,0,1],[-2,0,2],[-1,0,1]]`
- Gy kernel: `[[-1,-2,-1],[0,0,0],[1,2,1]]`
- Magnitude: `sqrt(Gx^2 + Gy^2)`, clamped to 0-255
- Includes configurable simulated delay (default 500ms) for demo purposes
- Adds `edgedetect_ms` to metadata

## ColorOverlay Processor

### Input
- Port "original": `ImageData` — original color image
- Port "edges": `ImageData` — edge map

### Output
- Port "composite": `ImageData` — neon edge overlay on original

### Algorithm
For each pixel:
- If edge brightness > threshold: use bright neon color (e.g., cyan/green)
- Else: use original pixel (optionally darkened)
- Result: original image with glowing edge lines
- Adds `overlay_ms` to metadata

## SepiaTransformer Processor

### Input
- Port "image": `ImageData`

### Output
- Port "result": `ImageData` — sepia-toned version

### Algorithm
For each pixel:
- newR = min(255, 0.393*R + 0.769*G + 0.189*B)
- newG = min(255, 0.349*R + 0.686*G + 0.168*B)
- newB = min(255, 0.272*R + 0.534*G + 0.131*B)
- Adds `sepia_ms` to metadata

## ImageViewer Sink

### Input
- Port "image": `ImageData`

### Output
None (sink node).

### Behavior
- Updates ViewModel state with received ImageData
- UI composable renders the ImageBitmap
- Displays metadata as text overlay or panel below image
- Shows per-node timing breakdown
