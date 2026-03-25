# Contract: Runtime Module Discovery

**Feature**: 060-separate-repo

## Overview

After separation, the graphEditor must discover project modules at runtime instead of having compile-time dependencies. This contract defines the interfaces for module discovery, session creation, and preview registration.

## Module Discovery Interface

The graphEditor discovers modules from the project directory at runtime.

### discoverModules(projectDir: File): List<ModuleInfo>

Scans the project directory for module directories containing `.flow.kt` files.

**Behavior**:
1. List subdirectories of `projectDir`
2. For each subdirectory, check if it contains `src/commonMain/kotlin/**/*.flow.kt`
3. If found, extract module name from the flow graph declaration
4. Return list of discovered module info (name, path, flow file path)

### ModuleInfo

| Field | Type | Description |
|-------|------|-------------|
| name | String | Module name (e.g., "WeatherForecast") |
| path | File | Absolute path to the module directory |
| flowFilePath | String | Path to the `.flow.kt` file |

## Session Factory Interface

The graphEditor creates runtime sessions for discovered modules without compile-time knowledge of their types.

### Current Pattern (hardcoded)

```
ModuleSessionFactory knows about each module at compile time:
  - imports StopWatchViewModel, UserProfilesViewModel, etc.
  - creates specific adapter objects for each ControllerInterface
```

### Target Pattern (discovery-based)

```
ModuleSessionFactory receives:
  - FlowGraph (from parsed .flow.kt)
  - CodeNodeDefinition lookup (from NodeDefinitionRegistry)
  - Module state/reset callback (optional, registered by module)

ModuleSessionFactory creates:
  - DynamicPipelineController (already exists, used for most modules)
  - Generic ViewModel that delegates to the controller
```

## Preview Registration Interface

Each module registers its own preview composable at startup.

### Current Pattern (hardcoded)

```
graphEditor/ui/StopWatchPreviewProvider.kt:
  PreviewRegistry.register("StopWatch") { viewModel -> StopWatchUI(viewModel) }
```

### Target Pattern (module-side registration)

```
StopWatch module provides:
  PreviewRegistry.register("StopWatch") { ... }

graphEditor discovers via ServiceLoader or classpath scanning.
```

## Koin Module Registration Interface

Project modules register their Koin dependency injection modules.

### Current Pattern

```
Main.kt: startKoin { modules(DatabaseModule, userProfilesModule, ...) }
```

### Target Pattern

```
Project provides a list of Koin modules via ServiceLoader or a known factory method.
graphEditor calls: startKoin { modules(discoveredKoinModules) }
```
