# graphEditor Module

Visual graph editor for CodeNodeIO flow-based programming using Compose Desktop.

## Architecture

The graphEditor follows the **ViewModel Pattern** with these key components:

### ViewModels

All business logic and state management is encapsulated in ViewModel classes located in `src/jvmMain/kotlin/viewmodel/`:

| ViewModel | Purpose | State Class |
|-----------|---------|-------------|
| `NodeGeneratorViewModel` | Custom node creation form | `NodeGeneratorPanelState` |
| `NodePaletteViewModel` | Node type browsing and search | `NodePaletteState` |
| `IPPaletteViewModel` | Information Packet type selection | `IPPaletteState` |
| `PropertiesPanelViewModel` | Node/connection property editing | `PropertiesPanelState` |
| `CanvasInteractionViewModel` | Canvas drag, selection, connections | `CanvasInteractionState` |
| `GraphEditorViewModel` | File operations, undo/redo, dialogs | `GraphEditorState` |

### Creating a New ViewModel

Follow this pattern when adding new UI components:

#### 1. Create State Data Class

```kotlin
// File: src/jvmMain/kotlin/viewmodel/MyNewViewModel.kt

data class MyNewState(
    val someValue: String = "",
    val isExpanded: Boolean = false
) : BaseState {
    // Computed properties for derived state
    val isValid: Boolean
        get() = someValue.isNotBlank()
}
```

#### 2. Create ViewModel Class

```kotlin
class MyNewViewModel(
    // Inject dependencies via constructor
    private val someRepository: SomeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MyNewState())
    val state: StateFlow<MyNewState> = _state.asStateFlow()

    // Actions - public methods that modify state
    fun setSomeValue(value: String) {
        _state.update { it.copy(someValue = value) }
    }

    fun toggleExpanded() {
        _state.update { it.copy(isExpanded = !it.isExpanded) }
    }

    fun reset() {
        _state.update { MyNewState() }
    }
}
```

#### 3. Update Composable to Use ViewModel

```kotlin
@Composable
fun MyNewPanel(
    viewModel: MyNewViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = modifier) {
        TextField(
            value = state.someValue,
            onValueChange = { viewModel.setSomeValue(it) }
        )

        Button(
            onClick = { /* call viewModel action */ },
            enabled = state.isValid
        ) {
            Text("Submit")
        }
    }
}
```

#### 4. Create ViewModel in Main.kt

```kotlin
// In GraphEditorApp composable
val myNewViewModel = remember {
    MyNewViewModel(someRepository)
}

// Pass to composable
MyNewPanel(viewModel = myNewViewModel)
```

#### 5. Write Unit Tests

```kotlin
class MyNewViewModelTest {
    @Test
    fun `setSomeValue updates state`() = runTest {
        val viewModel = MyNewViewModel(FakeSomeRepository())

        viewModel.setSomeValue("test")

        assertEquals("test", viewModel.state.first().someValue)
    }
}
```

### Key Principles

1. **State Immutability**: State classes are `data class` with `val` properties only
2. **Unidirectional Data Flow**: UI observes state, calls actions, state updates, UI re-renders
3. **No Business Logic in Composables**: Composables only render and delegate to ViewModel
4. **Testable Without Compose**: ViewModels can be unit tested without UI dependencies
5. **Dependencies via Constructor**: ViewModels receive dependencies through constructor injection

### Shared State

ViewModels that need access to shared resources use `SharedStateProvider`:

```kotlin
val LocalSharedState = staticCompositionLocalOf<SharedStateProvider> { ... }

data class SharedStateProvider(
    val graphState: GraphState,
    val undoRedoManager: UndoRedoManager,
    val propertyChangeTracker: PropertyChangeTracker,
    val ipTypeRegistry: IPTypeRegistry,
    val customNodeRepository: CustomNodeRepository
)
```

ViewModels communicate through `SharedStateProvider` rather than direct references to each other.

## Project Structure

```
graphEditor/
├── src/
│   └── jvmMain/kotlin/
│       ├── Main.kt                 # Application entry point
│       ├── viewmodel/              # ViewModels and state classes
│       │   ├── BaseState.kt
│       │   ├── SharedStateProvider.kt
│       │   ├── NodeGeneratorViewModel.kt
│       │   ├── NodePaletteViewModel.kt
│       │   ├── IPPaletteViewModel.kt
│       │   ├── PropertiesPanelViewModel.kt
│       │   ├── CanvasInteractionViewModel.kt
│       │   └── GraphEditorViewModel.kt
│       ├── ui/                     # Composable UI components
│       ├── state/                  # Core state (GraphState, UndoRedoManager)
│       └── repository/             # Data persistence
│   └── jvmTest/kotlin/
│       └── viewmodel/              # ViewModel unit tests
└── build.gradle.kts
```

## Running

```bash
./gradlew :graphEditor:run
```

## Testing

```bash
# Run all tests
./gradlew :graphEditor:jvmTest

# Run ViewModel tests only
./gradlew :graphEditor:jvmTest --tests "io.codenode.grapheditor.viewmodel.*"
```

## Dependencies

- Kotlin 2.1.21
- Compose Desktop 1.7.3
- JetBrains ViewModel: `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0`
- kotlinx-coroutines 1.8.0
- kotlinx-serialization 1.6.0
