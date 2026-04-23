# Quickstart Verification: Wire Code Generator Panel

**Feature**: 081-wire-codegen-panel
**Date**: 2026-04-23

## Verification Scenarios

### VS1: Generate Module via Panel

**Steps**: Open Code Generator panel → select "Generate Module" → click "Generate".
**Expected**: Module files written to output directory. Status message shows file count.

### VS2: Generate Repository via Panel

**Steps**: Code Generator panel → "Repository" → select IP Type → click "Generate".
**Expected**: Repository module files generated. Matches old "Create Repository Module" output.

### VS3: Generate UI-FBP via Panel

**Steps**: Code Generator panel → "UI-FBP" → select UI file → click "Generate".
**Expected**: UI-FBP interface files generated. Matches old "Generate UI-FBP" output.

### VS4: Selective Generation

**Steps**: Deselect "controller/" folder in file tree → click "Generate".
**Expected**: Controller files not generated. All other files present.

### VS5: Toolbar Buttons Removed

**Steps**: Inspect toolbar.
**Expected**: No "Generate Module" or "Generate UI-FBP" buttons. "Save" button present.

### VS6: Save Still Works

**Steps**: Click "Save".
**Expected**: Only .flow.kt written (unchanged behavior).

### VS7: Error Reporting

**Steps**: Trigger a generation with a known error condition.
**Expected**: Status message shows both successes and errors.
