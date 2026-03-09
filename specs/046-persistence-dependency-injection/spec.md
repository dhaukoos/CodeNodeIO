# Feature Specification: Persistence Dependency Injection

**Feature Branch**: `046-persistence-dependency-injection`
**Created**: 2026-03-09
**Status**: Draft
**Input**: User description: "Move only infrastructure to shared persistence module, use dependency injection for DAO access. Feature modules receive their DAOs from the app layer instead of calling DatabaseModule directly."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Decouple Feature Modules from Database Singleton (Priority: P1)

A developer maintaining the UserProfiles module notices that its processing logic directly calls a database singleton to obtain DAOs. This tight coupling means the module cannot function without the full database stack and makes testing difficult. The developer refactors the module so that DAOs are provided externally at startup — the module declares what data access it needs, and the application layer supplies it. After the change, the UserProfiles module no longer directly references the database singleton, yet all CRUD operations continue to work identically.

**Why this priority**: This is the core architectural change. Inverting the dependency from "module pulls its own DAO" to "app pushes DAO into module" breaks the coupling between feature modules and database infrastructure. Without this, entity/DAO classes cannot be moved back to their feature modules (US2) because it would recreate the circular dependency.

**Independent Test**: Build and run the application. Open the UserProfiles module in Runtime Preview, perform CRUD operations (Add, Update, Remove a profile). All operations succeed identically to before. Additionally, verify that the UserProfiles module source code contains zero direct references to the database singleton.

**Acceptance Scenarios**:

1. **Given** the UserProfiles module currently calls a database singleton directly, **When** the module is refactored to receive its DAO externally, **Then** the application builds successfully with no compilation errors
2. **Given** the DAO is provided externally at startup, **When** a user performs Add, Update, and Remove operations in Runtime Preview, **Then** all operations succeed identically to before
3. **Given** the refactoring is complete, **When** a developer inspects the UserProfiles module source code, **Then** there are zero direct references to the database singleton — all data access flows through the injected DAO
4. **Given** the DAO is provided externally, **When** the application starts in both desktop (graphEditor) and mobile (KMPMobileApp) contexts, **Then** the DAO is correctly wired in both environments

---

### User Story 2 - Move Entity and DAO Classes Back to Feature Module (Priority: P2)

A developer working on the persistence architecture notices that UserProfileEntity, UserProfileDao, and UserProfileRepository currently live in the shared persistence module, even though they are UserProfiles-specific domain types. Now that the feature module no longer depends on the database singleton (US1), the developer moves these domain-specific files back to the UserProfiles module. The shared persistence module retains only truly generic infrastructure (BaseDao, database builder utilities). After the move, each feature module owns its own domain types while sharing only the generic persistence infrastructure.

**Why this priority**: This completes the architectural vision — feature modules own their domain types, the shared persistence module contains only generic infrastructure. This is the payoff of US1's dependency inversion. However, it depends on US1 being complete since the entity/DAO classes can only move back once the circular dependency is broken.

**Independent Test**: Build and run the application. Verify UserProfileEntity, UserProfileDao, and UserProfileRepository exist in the UserProfiles module (not in the persistence module). Perform CRUD operations in Runtime Preview — all work identically.

**Acceptance Scenarios**:

1. **Given** entity/DAO classes currently reside in the shared persistence module, **When** they are moved back to the UserProfiles module, **Then** the application builds with zero compilation errors
2. **Given** the entity/DAO classes have been moved, **When** a developer inspects the shared persistence module, **Then** it contains only generic infrastructure — no feature-specific types
3. **Given** the entity/DAO classes have been moved, **When** a user performs CRUD operations in Runtime Preview, **Then** all operations work identically to before
4. **Given** the entity/DAO classes live in the UserProfiles module, **When** the shared persistence module's dependency graph is inspected, **Then** it has zero dependencies on feature modules

---

### User Story 3 - Centralize Database Assembly in the App Layer (Priority: P3)

A developer preparing the architecture for future entity modules (e.g., Orders, Inventory) needs the database assembly — the central registry that lists all entity types and constructs the database — to live in the application layer where all feature modules are known. The developer moves the database definition (which references all entity classes) and the database singleton to the app layer. Each application entry point (desktop, mobile) wires DAOs into feature modules at startup. After this change, adding a new entity module requires only: (1) creating the entity/DAO in the new module, (2) registering it in the app-layer database definition, and (3) wiring the DAO at startup.

**Why this priority**: This positions the architecture for multi-entity scalability. It depends on US1 (dependency injection) and US2 (entity classes in feature modules) being complete. The database definition must move last because it references entity classes from all feature modules.

**Independent Test**: Build and run the application. Verify the database definition and singleton live in the app layer. Perform CRUD operations — all work identically. Verify the shared persistence module has zero project dependencies.

**Acceptance Scenarios**:

1. **Given** the database definition currently lives in the shared persistence module, **When** it is moved to the app layer, **Then** the application builds with zero compilation errors
2. **Given** the database definition is in the app layer, **When** a user performs CRUD operations, **Then** all operations work identically
3. **Given** the database definition is in the app layer, **When** a developer inspects the shared persistence module, **Then** it has zero project dependencies — only generic library dependencies
4. **Given** the architecture is complete, **When** a developer wants to add a new entity module, **Then** they only need to create entity/DAO files in the new module and register them in the app-layer database definition

---

### Edge Cases

- What happens if the DAO is not provided before the module attempts data access? The module should fail with a clear initialization error rather than a null pointer or silent failure.
- What happens if the application has multiple entry points (desktop via graphEditor, mobile via KMPMobileApp) with different database configurations? Each entry point must independently wire DAOs at startup using its own database instance.
- What happens if a feature module is loaded but its entity type is not registered in the database definition? The application should fail at startup with a clear message identifying the missing registration.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Feature modules MUST receive their data access objects through an initialization mechanism rather than obtaining them from a global singleton
- **FR-002**: The initialization mechanism MUST support providing DAOs before the module's data processing begins
- **FR-003**: Feature modules MUST fail with a clear error if data access is attempted before initialization
- **FR-004**: Entity and DAO classes (UserProfileEntity, UserProfileDao, UserProfileRepository) MUST reside in their respective feature module, not in the shared persistence module
- **FR-005**: The shared persistence module MUST contain only generic persistence infrastructure with zero dependencies on feature modules
- **FR-006**: The database definition (which lists all entity types) MUST reside in the application layer where all feature modules are known
- **FR-007**: Each application entry point MUST wire DAOs into feature modules during startup
- **FR-008**: All existing UserProfiles CRUD functionality MUST continue to work identically after the refactoring
- **FR-009**: The dependency graph MUST be acyclic — no circular dependencies between modules

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The application builds with zero compilation errors after all refactoring is complete
- **SC-002**: 100% of existing UserProfiles CRUD operations (create, read, update, delete) work identically in Runtime Preview after the refactoring
- **SC-003**: The shared persistence module has zero project dependencies — it depends only on external libraries
- **SC-004**: The UserProfiles module contains zero direct references to the database singleton — all data access flows through injected DAOs
- **SC-005**: Feature-specific types (entity, DAO, repository) reside in their feature module, not in the shared persistence module
- **SC-006**: Adding a hypothetical new entity module requires changes in only 3 locations: the new module itself, the app-layer database definition, and the app-layer startup wiring

## Assumptions

- The dependency injection pattern used here is manual constructor/initialization injection — no DI framework is needed for the current scale of the project.
- "App layer" refers to the code that runs at each application entry point (graphEditor for desktop, KMPMobileApp for mobile). Both entry points must perform DAO wiring independently.
- The shared persistence module provides generic base types (e.g., BaseDao) and database builder utilities that any feature module can use without knowing about other feature modules.
- This is a pure structural refactoring — no new features, no behavior changes, no user-facing changes.
- The existing platform-specific database builder files (JVM, Android, iOS) remain in the persistence module since they are generic infrastructure for constructing any database.
- Feature 045 (which created the shared persistence module and moved all persistence files there) is the direct predecessor. This feature builds on that foundation.

## Scope

### In Scope

- Refactoring UserProfiles module to receive DAOs via initialization instead of calling a database singleton
- Moving entity/DAO classes (UserProfileEntity, UserProfileDao, UserProfileRepository) from the shared persistence module back to UserProfiles
- Moving the database definition and singleton to the application layer
- Wiring DAOs at startup in both graphEditor and KMPMobileApp entry points
- Updating all import paths across the codebase

### Out of Scope

- Adding new entity modules (future feature — this feature prepares the architecture)
- Introducing a DI framework (manual injection is sufficient)
- Modifying any node behavior or processing logic
- Changes to the database schema or stored data
- Adding unit tests for the persistence layer (no tests requested)

## Dependencies

- Feature 045 (refactor-userprofiles-module) — created the shared persistence module and established the current file layout
- Feature 039 (userprofiles-module) — established the UserProfiles module with CRUD operations
