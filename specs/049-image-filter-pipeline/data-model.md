# Data Model: Image Filter Pipeline

**Feature**: 049-image-filter-pipeline | **Date**: 2026-03-12

## Entities

### ImageData

The primary data packet flowing through the pipeline.

| Field | Type | Description |
|-------|------|-------------|
| bitmap | ImageBitmap | The image pixel data |
| width | Int | Image width in pixels |
| height | Int | Image height in pixels |
| metadata | Map<String, String> | Processing metadata (node timings, source info) |

### Metadata Keys (convention)

| Key | Example Value | Description |
|-----|---------------|-------------|
| source | "file:///path/to/image.jpg" | Where the image came from |
| grayscale_ms | "12" | GrayscaleTransformer processing time |
| edgedetect_ms | "534" | EdgeDetector processing time |
| overlay_ms | "8" | ColorOverlay processing time |
| sepia_ms | "15" | SepiaTransformer processing time |
| total_ms | "569" | Total pipeline processing time |

## Node Definitions

### ImagePicker (Source)

| Property | Value |
|----------|-------|
| Type | Source (0 inputs, 1 output) |
| Runtime | SourceRuntime<ImageData> |
| Output port | "image" : ImageData |
| Behavior | Opens file chooser, loads selected image, emits ImageData |

### GrayscaleTransformer (Processor)

| Property | Value |
|----------|-------|
| Type | Transformer (1 input, 1 output) |
| Runtime | TransformerRuntime<ImageData, ImageData> |
| Input port | "image" : ImageData |
| Output port | "result" : ImageData |
| Behavior | Converts color image to grayscale using luminosity formula |

### EdgeDetector (Processor)

| Property | Value |
|----------|-------|
| Type | Transformer (1 input, 1 output) |
| Runtime | TransformerRuntime<ImageData, ImageData> |
| Input port | "image" : ImageData |
| Output port | "edges" : ImageData |
| Behavior | Applies Sobel edge detection, outputs edge map. Includes configurable simulated delay (default 500ms) |

### ColorOverlay (Processor)

| Property | Value |
|----------|-------|
| Type | Merger (2 inputs, 1 output) |
| Runtime | In2Out1Runtime<ImageData, ImageData, ImageData> |
| Input port 1 | "original" : ImageData |
| Input port 2 | "edges" : ImageData |
| Output port | "composite" : ImageData |
| Behavior | Merges original image with edge map using additive color overlay |

### SepiaTransformer (Processor)

| Property | Value |
|----------|-------|
| Type | Transformer (1 input, 1 output) |
| Runtime | TransformerRuntime<ImageData, ImageData> |
| Input port | "image" : ImageData |
| Output port | "result" : ImageData |
| Behavior | Applies sepia tone transformation matrix |

### ImageViewer (Sink)

| Property | Value |
|----------|-------|
| Type | Sink (1 input, 0 outputs) |
| Runtime | SinkRuntime<ImageData> |
| Input port | "image" : ImageData |
| Behavior | Renders image and displays processing metadata in UI |

## Flow Graph Connections

```
ImagePicker.image ──→ GrayscaleTransformer.image
ImagePicker.image ──→ ColorOverlay.original        (fan-out)
GrayscaleTransformer.result ──→ EdgeDetector.image
EdgeDetector.edges ──→ ColorOverlay.edges           (fan-in)
ColorOverlay.composite ──→ ImageViewer.image
```

## State Transitions

```
Idle (no image selected)
    └── User selects image via ImagePicker
        └── Processing (image flows through pipeline)
            ├── GrayscaleTransformer processes
            ├── EdgeDetector processes (may take 500ms+)
            └── ColorOverlay waits for both inputs
                └── Complete (composite image rendered in ImageViewer)
                    └── User selects new image → Processing
```
