# Vertical Slice Migration Map

**Feature**: 064-vertical-slice-refactor
**Date**: 2026-04-02

## Cross-Module Seam Summary

Three source modules are being decomposed into five vertical-slice target modules. This section documents the cross-module seams — dependencies that cross the boundaries between graphEditor, kotlinCompiler, and circuitSimulator.

### Source Module Dependency Direction

```
graphEditor ──depends on──► kotlinCompiler (code generation)
graphEditor ──depends on──► circuitSimulator (runtime session)
graphEditor ──depends on──► fbpDsl (shared vocabulary)
kotlinCompiler ──depends on──► fbpDsl (shared vocabulary)
circuitSimulator ──depends on──► fbpDsl (shared vocabulary)
```

**Key finding**: Dependencies flow **one-way** from graphEditor to the other two modules. Neither kotlinCompiler nor circuitSimulator imports from graphEditor or from each other. This simplifies extraction — each satellite module can be moved independently.

### graphEditor → kotlinCompiler Seams

| graphEditor File | kotlinCompiler File | Type | graphEditor Bucket | Target Module |
|-----------------|-------------------|------|-------------------|---------------|
| save/ModuleSaveService.kt | generator/KotlinCodeGenerator.kt | Function call | generate | flowGraph-generate |
| save/ModuleSaveService.kt | generator/ModuleGenerator.kt | Function call | generate | flowGraph-generate |
| save/ModuleSaveService.kt | generator/FlowKtGenerator.kt | Function call | generate | flowGraph-generate |
| compilation/CompilationService.kt | validator/LicenseValidator.kt | Function call | generate | flowGraph-generate |

**Resolution**: All graphEditor files that depend on kotlinCompiler are themselves in the `generate` bucket. When both move to flowGraph-generate, these cross-module seams become internal dependencies. **No new interfaces needed.**

### graphEditor → circuitSimulator Seams

| graphEditor File | circuitSimulator File | Type | graphEditor Bucket | Target Module |
|-----------------|---------------------|------|-------------------|---------------|
| ui/RuntimePreviewPanel.kt | RuntimeSession.kt | Function call | execute | flowGraph-execute |
| ui/ModuleSessionFactory.kt | RuntimeSession.kt | Function call | execute | flowGraph-execute |
| ui/FlowGraphCanvas.kt | ConnectionAnimation.kt | Type reference | root | root→execute |

**Resolution**: RuntimePreviewPanel and ModuleSessionFactory are in the `execute` bucket — they move to flowGraph-execute alongside circuitSimulator files, so those seams become internal. FlowGraphCanvas.kt references ConnectionAnimation for rendering animated dots; this is the one true cross-module interface needed (root→execute boundary).

### Consolidated Seam Counts

| Boundary | Seam Count | Post-Extraction |
|----------|-----------|-----------------|
| graphEditor(generate) → kotlinCompiler | 4 | Internal to flowGraph-generate |
| graphEditor(execute) → circuitSimulator | 2 | Internal to flowGraph-execute |
| graphEditor(root) → circuitSimulator | 1 | Interface needed: root→execute (ConnectionAnimation) |
| kotlinCompiler → circuitSimulator | 0 | No dependency |
| circuitSimulator → kotlinCompiler | 0 | No dependency |
| **Total cross-module seams** | **7** | **1 interface needed** |

### Audit File Counts

| Source Module | File Count | Bucket Distribution |
|--------------|-----------|-------------------|
| graphEditor | 77 | root: 28, inspect: 17, persist: 13, compose: 9, generate: 7, execute: 3 |
| kotlinCompiler | 38 | generate: 38 |
| circuitSimulator | 5 | execute: 5 |
| **Total** | **120** | |

## Module Boundaries

_Populated in US3 — file assignments per target module_

## Public APIs

_Populated in US3 — Kotlin interfaces per module_

## Extraction Order

_Populated in US3 — step-by-step extraction sequence_

## Step-by-Step Instructions

_Populated in US3 — detailed instructions per extraction step_
