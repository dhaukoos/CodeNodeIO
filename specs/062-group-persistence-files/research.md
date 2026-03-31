# Research: Group Persistence Files by Entity

**Feature**: 062-group-persistence-files
**Date**: 2026-03-31

## Decision 1: Subdirectory and Package Naming Convention

**Decision**: PascalCase directory name matching entity name; lowercase package name.
- Directory: `UserProfile/`, `Address/`, `TestItem/`
- Package: `io.codenode.persistence.userprofile`, `io.codenode.persistence.address`

**Rationale**: Kotlin convention uses lowercase package names. PascalCase directory names provide visual clarity when browsing the file tree, while the package names follow standard Kotlin style. This matches how other KMP projects organize sub-packages.

**Alternatives considered**:
- Lowercase directory names (`userprofile/`) — rejected because the entity name is PascalCase everywhere else in the project and PascalCase dirs are already the convention for module directories (e.g., `UserProfiles/`, `Addresses/`)
- Plural directory names (`UserProfiles/`) — rejected because the grouping is by entity type, not module name

## Decision 2: Where to Derive Entity Sub-Package

**Decision**: Derive from `EntityModuleSpec.persistencePackage` + `entityName.lowercase()` at the generator call sites. No new field on `EntityModuleSpec`.

**Rationale**: The sub-package is a simple derivation (`"$persistencePackage.${entityName.lowercase()}"`). Adding a new field would increase API surface with no flexibility benefit since the convention is deterministic.

**Alternatives considered**:
- Add `persistenceEntityPackage` field to `EntityModuleSpec` — rejected as unnecessary complexity for a derived value
- Use `entityName` as-is for sub-package (mixed case) — rejected as it violates Kotlin naming conventions

## Decision 3: Module Removal Strategy

**Decision**: Replace per-file deletion with `File(persistenceDir, entityName).deleteRecursively()`. Update AppDatabase regeneration to scan subdirectories.

**Rationale**: The current removal deletes three files by name. With subdirectories, deleting the directory is simpler and handles any future additions to the entity triplet. The AppDatabase regeneration already scans for `*Entity.kt` files — it just needs to recurse into subdirectories.

**Alternatives considered**:
- Keep per-file deletion but with subdirectory paths — rejected as more fragile and doesn't handle unexpected extra files in the entity directory

## Decision 4: AppDatabase Entity Scanning

**Decision**: Change the entity scanning in `removeEntityModule()` / AppDatabase regeneration from flat `persistenceDir.listFiles()` to a recursive walk that finds `*Entity.kt` in immediate subdirectories.

**Rationale**: After the change, entity files live in `persistenceDir/{Entity}/{Entity}Entity.kt`. The regeneration logic needs to find them to build the `@Database(entities = [...])` annotation. A shallow recursive scan (depth 1) is sufficient since entities are always one level deep.

**Alternatives considered**:
- Full recursive walk — rejected as unnecessarily broad; entities are always at depth 1
- Maintain a manifest file listing entities — rejected as added complexity when filesystem scanning works

## Decision 5: Import Update Strategy for DemoProject

**Decision**: Manually update all import statements in DemoProject files as part of this feature. The change is:
- Before: `import io.codenode.persistence.{Entity}{Suffix}`
- After: `import io.codenode.persistence.{entitylowercase}.{Entity}{Suffix}`

**Rationale**: There are exactly 9 files across 2 modules (UserProfiles, Addresses) plus 2 shared files (AppDatabase, PersistenceBootstrap). This is a small, bounded change that can be done with search-and-replace.

**Files affected in DemoProject** (enumerated):
1. `UserProfiles/.../UserProfilesViewModel.kt` — 3 imports
2. `UserProfiles/.../UserProfilesPersistence.kt` — 2 imports
3. `UserProfiles/.../UserProfileConverters.kt` — 1 import
4. `UserProfiles/.../nodes/UserProfileRepositoryCodeNode.kt` — 1 import
5. `Addresses/.../AddressesPersistence.kt` — 2 imports
6. `Addresses/.../AddressesViewModel.kt` — 2 imports
7. `Addresses/.../AddressConverters.kt` — 1 import
8. `Addresses/.../nodes/AddressRepositoryCodeNode.kt` — 1 import
9. `persistence/.../AppDatabase.kt` — 2 imports
10. `persistence/.../PersistenceBootstrap.kt` — 2 imports
