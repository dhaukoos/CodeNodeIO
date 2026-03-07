# Quickstart: Animate Data Flow

**Feature**: 041-animate-data-flow
**Date**: 2026-03-06

## Prerequisites

- graphEditor compiles and launches: `./gradlew :graphEditor:run`
- StopWatch and UserProfiles modules are loadable
- Runtime Preview panel works (start/stop/pause/resume/attenuation)

## Verification Steps

### V1: Toggle Visibility and Threshold Gating

1. Launch graphEditor and load the StopWatch module
2. Expand the Runtime Preview panel
3. **Verify**: An "Animate Data Flow" toggle appears in the Speed Attenuation section, below the slider range labels
4. **Verify**: With attenuation at 0ms, the toggle is disabled (grayed out)
5. Move the attenuation slider to 500ms
6. **Verify**: The toggle becomes enabled
7. Move attenuation back to 400ms
8. **Verify**: The toggle is disabled again

### V2: Toggle Activation and Deactivation

1. Set attenuation to 1000ms
2. Enable the "Animate Data Flow" toggle
3. **Verify**: Toggle shows as active (checked/on state)
4. Reduce attenuation to 300ms
5. **Verify**: Toggle is automatically deactivated and disabled
6. Increase attenuation to 600ms
7. **Verify**: Toggle is enabled but remains off (not auto-activated)

### V3: Basic Dot Animation (StopWatch)

1. Set attenuation to 1000ms and enable "Animate Data Flow"
2. Click Start to begin execution
3. **Verify**: Small dots appear and travel along connection curves from output ports to input ports
4. **Verify**: Dots travel the full length of each curve, appearing at the source and disappearing at the target
5. **Verify**: Each dot completes its journey in approximately 800ms (80% of 1000ms)
6. **Verify**: Multiple connections animate simultaneously without interference

### V4: Pause and Resume

1. With execution running and dots animating, click Pause
2. **Verify**: Any in-progress dots freeze in place on their curves
3. Click Resume
4. **Verify**: Frozen dots resume their journey from where they paused
5. **Verify**: New dots begin animating as data flows again

### V5: Stop Clears Animations

1. With execution running and dots animating, click Stop
2. **Verify**: All dots disappear immediately (no lingering animations)

### V6: Toggle Off During Execution

1. Start execution with animation enabled (attenuation 1000ms)
2. While dots are animating, disable the "Animate Data Flow" toggle
3. **Verify**: Any in-progress dots complete their current animation
4. **Verify**: No new dots appear after toggle is disabled
5. **Verify**: Execution continues normally without animation

### V7: Module-Agnostic Animation (UserProfiles)

1. Load the UserProfiles module
2. Set attenuation to 1000ms and enable "Animate Data Flow"
3. Start execution
4. **Verify**: Dots animate along UserProfiles' connection curves
5. **Verify**: Visual behavior is identical to StopWatch (same dot size, timing, movement)

### V8: Module Switch Clears Animations

1. With StopWatch running and dots animating, switch to UserProfiles module
2. **Verify**: All StopWatch animations are cleared immediately
3. **Verify**: No stale animations from the previous module

## Expected Visual Behavior

- **Dot size**: Approximately twice the connection line width (~6px diameter at default scale)
- **Dot color**: Matches the connection curve color (IP type color or default dark gray)
- **Dot movement**: Smooth parametric travel along the cubic Bezier curve
- **Timing**: 80% of current attenuation value (e.g., 800ms at 1000ms attenuation)
- **Rendering**: Dots render on top of connection curves but behind nodes
