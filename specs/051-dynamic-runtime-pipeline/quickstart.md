# Quickstart: Dynamic Runtime Pipeline

## Validation Steps

### Step 1: Verify EdgeArtFilter runs with dynamic pipeline

1. Launch the graphEditor
2. Open (load) the EdgeArtFilter module
3. Verify all 6 EdgeArtFilter nodes appear on the canvas (ImagePicker, GrayscaleTransformer, EdgeDetector, ColorOverlay, ImageViewer, SepiaTransformer)
4. Press **Start** in the Runtime Preview panel
5. Click **Pick Image** and select a test image
6. **Expected**: Processed image appears with neon cyan edges, identical to pre-migration output
7. Press **Stop**

### Step 2: Verify hot-swap works

1. With EdgeArtFilter loaded and stopped, delete the GrayscaleTransformer node from the canvas
2. Drag SepiaTransformer from the palette onto the canvas
3. Re-wire: connect ImagePicker output1 → SepiaTransformer input, SepiaTransformer output → EdgeDetector input
4. Press **Start**
5. Click **Pick Image** and select the same test image
6. **Expected**: Output shows sepia-toned edges instead of grayscale edges
7. Press **Stop**

### Step 3: Verify error handling for unresolvable nodes

1. Create a new canvas with a node whose name does not match any registered CodeNodeDefinition
2. Press **Start**
3. **Expected**: Error message appears identifying the unresolvable node; pipeline does not start

### Step 4: Verify StopWatch fallback

1. Load the StopWatch module
2. Press **Start** in the Runtime Preview panel
3. **Expected**: StopWatch runs normally using its existing generated Controller/Flow
4. Verify timer ticks, pause/resume work, stop returns to idle

### Step 5: Verify all existing modules still work

1. Load UserProfiles → Start → verify CRUD operations work → Stop
2. Load GeoLocations → Start → verify map/location display works → Stop
3. Load Addresses → Start → verify address list works → Stop
4. **Expected**: All modules function identically to before this feature

### Step 6: Verify lifecycle operations on dynamic pipeline

1. Load EdgeArtFilter, press **Start**, pick an image
2. Press **Pause** → verify state shows "Paused", no new processing
3. Press **Resume** → verify processing resumes
4. Set Speed Attenuation to 400ms → verify pipeline slows down
5. Enable Animate Data Flow → verify dot animations appear on connections
6. Press **Stop** → verify clean shutdown, state returns to "Idle"
7. Press **Start** again → verify pipeline restarts cleanly
