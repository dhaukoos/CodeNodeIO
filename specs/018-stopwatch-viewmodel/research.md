# Research: StopWatch ViewModel Pattern

**Feature**: 018-stopwatch-viewmodel
**Date**: 2026-02-16

## Research Questions

### 1. JetBrains lifecycle-viewmodel-compose Compatibility with KMPMobileApp

**Decision**: Use `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0`

**Rationale**:
- Already proven to work in graphEditor module (feature 017-viewmodel-pattern)
- Version 2.8.0 is compatible with Kotlin 2.1.21 and Compose Multiplatform 1.7.3
- Provides platform-agnostic ViewModel class that works on Android, iOS, and Desktop
- Apache 2.0 licensed (compliant with constitution)

**Alternatives Considered**:
- `moko-mvvm` - More features but adds unnecessary dependency complexity
- Custom ViewModel implementation - Reinvents the wheel, no lifecycle integration
- Android-only ViewModel - Not multiplatform compatible

### 2. State Exposure Pattern

**Decision**: Expose StopWatchController's existing StateFlow properties directly through ViewModel

**Rationale**:
- StopWatchController already exposes `elapsedSeconds: StateFlow<Int>`, `elapsedMinutes: StateFlow<Int>`, and `executionState: StateFlow<ExecutionState>`
- No transformation needed - these can be delegated directly
- StateFlow supports late subscription (current value immediately available)
- Consistent with graphEditor ViewModel pattern

**Alternatives Considered**:
- Create StopWatchState data class combining all fields - Adds indirection, more complex updates
- Use MutableStateFlow in ViewModel - Would duplicate state, synchronization issues
- Transform to different types - No benefit, adds complexity

### 3. Action Delegation Pattern

**Decision**: ViewModel methods directly delegate to StopWatchController methods

**Rationale**:
- StopWatchController already has `start()`, `stop()`, `pause()`, `reset()` methods
- No additional business logic needed in ViewModel layer
- Clean separation - ViewModel is pure facade for UI
- Allows unit testing by mocking StopWatchController

**Code Pattern**:
```kotlin
class StopWatchViewModel(
    private val controller: StopWatchController
) : ViewModel() {

    val elapsedSeconds: StateFlow<Int> = controller.elapsedSeconds
    val elapsedMinutes: StateFlow<Int> = controller.elapsedMinutes
    val executionState: StateFlow<ExecutionState> = controller.executionState

    fun start() = controller.start()
    fun stop() = controller.stop()
    fun reset() = controller.reset()
}
```

### 4. ViewModel Creation and Lifecycle

**Decision**: Create StopWatchViewModel in App.kt using `viewModel { }` factory, pass to StopWatch composable

**Rationale**:
- `viewModel { }` handles lifecycle correctly on all platforms
- StopWatchController creation requires FlowGraph, which App.kt already manages
- Passing ViewModel as parameter enables testability (can inject mock)
- Matches graphEditor pattern where ViewModels are created at composition root

**Alternatives Considered**:
- Create in StopWatch composable - Harder to test, lifecycle concerns
- Use CompositionLocal - More complex, not needed for single ViewModel
- Static/singleton - Anti-pattern, not testable

### 5. Testing Strategy

**Decision**: Unit test ViewModel with mock StopWatchController

**Rationale**:
- ViewModel is a thin facade - testing delegation is sufficient
- Mock controller can simulate state changes and verify action calls
- Tests run without Compose UI dependencies (SC-002)
- Can test edge cases (rapid start/stop, late subscription)

**Test Pattern**:
```kotlin
class StopWatchViewModelTest {
    @Test
    fun `start delegates to controller`() = runTest {
        val mockController = MockStopWatchController()
        val viewModel = StopWatchViewModel(mockController)

        viewModel.start()

        assertTrue(mockController.startCalled)
    }
}
```

## Technical Decisions Summary

| Decision | Choice | Confidence |
|----------|--------|------------|
| ViewModel Library | JetBrains lifecycle-viewmodel-compose 2.8.0 | High |
| State Pattern | Delegate controller StateFlow directly | High |
| Action Pattern | Direct method delegation | High |
| Creation Location | App.kt with viewModel { } factory | High |
| Testing Approach | Mock controller, verify delegation | High |

## Dependencies to Add

```kotlin
// KMPMobileApp/build.gradle.kts - commonMain.dependencies
implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

// KMPMobileApp/build.gradle.kts - commonTest.dependencies
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
```

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| ViewModel library incompatibility with iOS | Low | High | Already working in graphEditor; test on iOS simulator early |
| StateFlow subscription timing issues | Low | Medium | StateFlow guarantees current value on collect |
| Lifecycle not managed correctly | Low | Medium | Use viewModel { } factory, test lifecycle events |
