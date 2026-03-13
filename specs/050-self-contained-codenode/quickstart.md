# Quickstart: Self-Contained CodeNode Definition

**Feature**: 050-self-contained-codenode | **Date**: 2026-03-13

## Overview

This feature replaces the fragmented node definition pattern (separate CustomNodeDefinition + ProcessingLogic + generated runtime references) with a single-file self-contained node class. The Node Generator produces an editable file, the graph editor auto-discovers it, and the runtime resolves processing logic by node name.

## Implementation Steps

### Step 1: Define the CodeNodeDefinition interface

Create the interface in `fbpDsl` that all self-contained nodes implement:
- Properties: name, category, inputPorts, outputPorts, description
- Methods: `createRuntime(name)`, `toNodeTypeDefinition()`
- Helper data class: `PortSpec(name, dataType)`

### Step 2: Create the nodes module

Add a new `nodes` Gradle module for project-level node definitions:
- Configure as KMP module with fbpDsl dependency
- This is the default target for generated nodes

### Step 3: Build NodeDefinitionRegistry

Create a registry that discovers nodes from three sources on startup:
- Classpath: compiled CodeNodeDefinition implementations
- Filesystem: Universal-level templates at `~/.codenode/nodes/`
- Legacy: existing CustomNodeDefinition entries from JSON

### Step 4: Update the Node Generator

Extend the existing NodeGeneratorPanel and ViewModel to:
- Add category selector (Source, Transformer, Processor, Sink)
- Add placement level selector (Module, Project, Universal)
- Generate a `{NodeName}CodeNode.kt` file with processing logic placeholder
- Check for name conflicts across all levels

### Step 5: Wire registry into graph editor

Replace the hardcoded node registration in Main.kt with:
- NodeDefinitionRegistry.discoverAll() on startup
- Registry provides NodeTypeDefinitions for palette display
- Registry lookup replaces CustomNodeRepository for self-contained nodes

### Step 6: Update runtime resolution

Modify the runtime flow execution to:
- Look up node definitions by name from the registry
- Call `createRuntime()` to get the appropriate NodeRuntime with processing logic
- Wire channels between runtimes as before

### Step 7: Verify hot-swap

Test the complete workflow:
1. Generate SepiaTransformerCodeNode at Project level
2. Add sepia processing logic
3. Build the project
4. Open EdgeArtFilter flow graph
5. Replace GrayscaleTransformer with SepiaTransformer on canvas
6. Run runtime preview → verify sepia output

### Step 8: Migrate EdgeArtFilter nodes

Convert all 6 EdgeArtFilter nodes from the legacy pattern to self-contained format:

1. Create `EdgeArtFilter/src/.../nodes/` directory for module-level nodes
2. For each node, create a `{NodeName}CodeNode.kt` object implementing `CodeNodeDefinition`:
   - **ImagePickerCodeNode** (Source: 0 in, 2 out) — embed `imagePickerGenerate` logic
   - **GrayscaleTransformerCodeNode** (Transformer: 1 in, 1 out) — embed `grayscaleTransform` logic
   - **EdgeDetectorCodeNode** (Transformer: 1 in, 1 out) — embed `edgeDetectorTransform` logic
   - **ColorOverlayCodeNode** (Processor: 2 in, 1 out) — embed `colorOverlayProcess` logic
   - **SepiaTransformerCodeNode** (Transformer: 1 in, 1 out) — embed `sepiaTransform` logic
   - **ImageViewerCodeNode** (Sink: 1 in, 0 out) — embed `imageViewerConsume` logic
3. Remove the hardcoded `edgeArtFilterNodes` list and color-coding overrides from Main.kt
4. Remove the legacy `processingLogic/` files
5. Verify the EdgeArtFilter pipeline runs identically via runtime preview

## Key Files

| File | Change |
|------|--------|
| `fbpDsl/src/.../runtime/CodeNodeDefinition.kt` | New: interface + PortSpec |
| `nodes/build.gradle.kts` | New: project-level nodes module |
| `graphEditor/src/.../state/NodeDefinitionRegistry.kt` | New: discovery + lookup |
| `graphEditor/src/.../viewmodel/NodeGeneratorViewModel.kt` | Modified: category, level, file generation |
| `graphEditor/src/.../ui/NodeGeneratorPanel.kt` | Modified: category + level UI |
| `graphEditor/src/.../Main.kt` | Modified: registry replaces hardcoded registration |
| `EdgeArtFilter/src/.../nodes/*.kt` | New: 6 migrated self-contained node files |
| `EdgeArtFilter/src/.../processingLogic/*.kt` | Removed: 6 legacy processing logic files |

## Example: Generated SepiaTransformerCodeNode.kt

```kotlin
package io.codenode.nodes

import io.codenode.fbpdsl.runtime.*

object SepiaTransformerCodeNode : CodeNodeDefinition {
    override val name = "SepiaTransformer"
    override val category = NodeCategory.TRANSFORMER
    override val description = "Transformer node with 1 input and 1 output"
    override val inputPorts = listOf(PortSpec("input1", Any::class))
    override val outputPorts = listOf(PortSpec("output1", Any::class))

    // TODO: Replace with your processing logic
    private val transformBlock: ContinuousTransformBlock<Any, Any> = { input -> input }

    override fun createRuntime(name: String): NodeRuntime<*> {
        return CodeNodeFactory.createContinuousTransformer(
            name = name,
            transform = transformBlock
        )
    }
}
```
