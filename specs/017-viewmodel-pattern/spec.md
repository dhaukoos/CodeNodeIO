# Feature Specification: ViewModel Pattern Migration

**Feature Branch**: `017-viewmodel-pattern`
**Created**: 2026-02-16
**Status**: Draft
**Input**: User description: "Use ViewModel Pattern - Change the State Management Pattern to the ViewModel Pattern throughout the codebase of the graphEditor."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consistent State Management Across Editor (Priority: P1)

Developers maintaining the graphEditor need a consistent, predictable state management pattern. Currently, state is managed through a mix of `GraphState` class with `mutableStateOf` properties and scattered `remember { mutableStateOf() }` calls throughout UI composables. The ViewModel pattern provides a standardized approach where each major UI component has a dedicated ViewModel that encapsulates its state and business logic.

**Why this priority**: Consistent patterns reduce cognitive load for developers, make the codebase easier to navigate, and establish a foundation for all other improvements.

**Independent Test**: After migration, every UI component's state and logic can be traced to a single ViewModel class, and no business logic remains in composable functions.

**Acceptance Scenarios**:

1. **Given** a developer examining the codebase, **When** they look at any UI component (e.g., NodePalette, PropertiesPanel, FlowGraphCanvas), **Then** they can identify a corresponding ViewModel that manages all state for that component.
2. **Given** a composable function, **When** examining its implementation, **Then** it contains only UI rendering logic with no direct state mutations or business calculations.

---

### User Story 2 - Improved Testability of Editor Logic (Priority: P2)

Developers need to write unit tests for editor behavior without requiring UI framework dependencies. ViewModels that separate state management from UI rendering enable testing business logic in isolation.

**Why this priority**: Testability is essential for maintaining quality as the editor grows, but requires the foundational pattern (P1) to be in place first.

**Independent Test**: Unit tests can be written for ViewModel classes that verify state changes, business logic, and side effects without any Compose UI dependencies.

**Acceptance Scenarios**:

1. **Given** a ViewModel class, **When** a developer writes a test, **Then** the test can instantiate the ViewModel and verify behavior without any UI framework imports.
2. **Given** a complex user interaction (e.g., undo/redo, node creation), **When** tested through the ViewModel, **Then** all state changes can be verified through ViewModel properties.

---

### User Story 3 - Modular Component Architecture (Priority: P3)

As the editor grows, developers need to add new features without modifying central state management. The ViewModel pattern enables modular development where new components can be added with their own ViewModels.

**Why this priority**: Modularity enables future growth but builds on the consistency and testability foundations.

**Independent Test**: A new UI component can be added with its own ViewModel without modifying existing ViewModel classes.

**Acceptance Scenarios**:

1. **Given** a requirement to add a new panel to the editor, **When** implementing it, **Then** the developer creates a dedicated ViewModel without touching existing ViewModels.
2. **Given** multiple ViewModels in the system, **When** they need to communicate, **Then** they do so through well-defined interfaces, not direct property access.

---

### Edge Cases

- What happens when ViewModels need to share state (e.g., selection state used by multiple panels)?
- How does the system handle ViewModel lifecycle when panels are shown/hidden?
- What happens when undo/redo operations span multiple ViewModels?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Each major UI component MUST have a corresponding ViewModel that encapsulates all its state
- **FR-002**: Composable functions MUST NOT contain business logic; they MUST only render UI based on ViewModel state
- **FR-003**: ViewModels MUST expose state as observable properties that trigger UI updates when changed
- **FR-004**: ViewModels MUST expose actions as methods that can be called from the UI
- **FR-005**: Shared state (e.g., current selection, active graph) MUST be managed through a coordinating mechanism accessible to relevant ViewModels
- **FR-006**: The existing undo/redo functionality MUST continue to work correctly after migration
- **FR-007**: All existing editor functionality MUST behave identically after migration
- **FR-008**: ViewModels MUST be instantiable without UI framework dependencies for testing

### Key Entities

- **ViewModel**: Encapsulates state and business logic for a UI component; exposes observable state and action methods
- **SharedState**: Centralized state that multiple ViewModels need to access (e.g., current FlowGraph, selection)
- **UIEvent**: User interactions from composables that trigger ViewModel actions
- **StateFlow**: Observable state stream that composables subscribe to for reactive updates

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of scattered `remember { mutableStateOf() }` calls for business state are moved to ViewModels
- **SC-002**: All existing automated tests pass after migration
- **SC-003**: All existing editor features work identically from a user perspective
- **SC-004**: Every ViewModel can be instantiated and tested without Compose UI imports
- **SC-005**: Code review confirms no business logic remains in composable functions
- **SC-006**: New ViewModel classes follow a consistent naming convention (e.g., `*ViewModel`)

## Assumptions

- The migration will preserve all existing editor functionality without user-visible changes
- ViewModels will use Compose-compatible state mechanisms (StateFlow or mutableStateOf) for reactivity
- The existing `GraphState` class will be refactored/split into appropriate ViewModels
- Shared state coordination will use a pattern compatible with dependency injection
- The migration can be done incrementally, one component at a time
