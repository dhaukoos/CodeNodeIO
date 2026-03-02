# Quickstart: Entity Repository Nodes

**Feature**: 036-entity-repository-nodes
**Date**: 2026-03-01

## Scenario 1: View Custom IP Type Properties (US1)

### Setup
1. Launch the Graph Editor application
2. Create a custom IP Type "User" via the IP Generator with properties:
   - name: String (required)
   - email: String (required)
   - age: Int (optional)

### Steps
1. In the IP Types palette (left panel), click on "User"
2. Observe the Properties Panel (right panel)

### Expected Result
- Properties Panel displays:
  - Header: "IP Type Properties"
  - Type name: "User"
  - Color swatch (matching the type's color)
  - Properties section:
    - `name` — String — Required
    - `email` — String — Required
    - `age` — Int — Optional

### Verify Selection Switching
1. Click on "Int" (built-in type) in the palette
2. Properties Panel shows: type name "Int", color swatch, description — no Properties section
3. Click a node on the canvas
4. Properties Panel switches to show node properties (IP type selection cleared)

---

## Scenario 2: Create Repository Node from Custom IP Type (US2)

### Setup
1. Have a custom IP Type "Product" with properties:
   - productId: Int (required)
   - name: String (required)
   - price: Double (required)
   - description: String (optional)

### Steps
1. Select "Product" in the IP Types palette
2. In the Properties Panel, observe the "Create Repository Node" button
3. Click "Create Repository Node"

### Expected Result
- A new node "ProductRepository" appears in the Node Palette
- The node has:
  - Input ports: save (Product), update (Product), remove (Product)
  - Output ports: result (Product), error (String)
- The "Create Repository Node" button now shows "Repository exists" (disabled)

### Verify Node Placement
1. Drag "ProductRepository" from the Node Palette onto the canvas
2. Verify all 5 ports display with correct names and type colors
3. Connect the "error" output to a sink node to verify connectivity

### Verify Duplicate Prevention
1. Select "Product" in the IP Types palette again
2. The "Create Repository Node" button should be disabled
3. Tooltip/label should indicate the repository already exists

---

## Scenario 3: Code Generation with Repository Node (US3)

### Setup
1. Create custom IP Type "Task" with properties:
   - title: String (required)
   - completed: Boolean (required)
   - priority: Int (optional)
2. Create "TaskRepository" node from the "Task" IP type
3. Place "TaskRepository" on a new flow graph
4. Connect a generator node to the "save" input port
5. Connect the "result" output to a sink node
6. Connect the "error" output to another sink node

### Steps
1. Save the flow graph (triggers code generation)

### Expected Result
Generated module contains in `persistence/` package:
- `BaseDao.kt` — Generic interface with insert/update/delete
- `TaskEntity.kt` — Data class with @Entity, @PrimaryKey(autoGenerate), columns: id, title, completed, priority
- `TaskDao.kt` — Interface extending BaseDao<TaskEntity> with getAllAsFlow() query
- `TaskRepository.kt` — Class wrapping TaskDao with save/update/remove/observeAll methods
- `AppDatabase.kt` — @Database class with TaskEntity registered, abstract taskDao() method

Runtime wiring in generated Flow class:
- save input channel → `scope.launch { repository.save(item) }`
- update input channel → `scope.launch { repository.update(item) }`
- remove input channel → `scope.launch { repository.remove(item) }`
- result output ← `repository.observeAll().collect { list -> resultChannel.send(list) }`
- error output ← try/catch wrapping operations, sending error messages

---

## Scenario 4: Multiple Repository Nodes with Shared Database (US4)

### Setup
1. Create two custom IP Types:
   - "User" (name: String, email: String)
   - "Order" (orderId: Int, userId: Int, total: Double)
2. Create "UserRepository" and "OrderRepository" nodes
3. Place both on the same flow graph

### Steps
1. Save the flow graph

### Expected Result
- Single `AppDatabase.kt` with both entities:
  ```
  @Database(entities = [UserEntity::class, OrderEntity::class], version = 1)
  ```
- Both DAOs as abstract methods in the database class
- Single `DatabaseModule` singleton providing the shared database instance
- Both repositories reference the same database via `DatabaseModule.getDatabase()`
- Platform-specific `DatabaseBuilder.{platform}.kt` files generated for JVM/Android/iOS

---

## Scenario 5: Edge Case — Deleted Source IP Type (Edge)

### Setup
1. Create custom IP Type "Temporary"
2. Create "TemporaryRepository" node
3. Delete the "Temporary" IP type from the IP palette

### Expected Result
- "TemporaryRepository" remains in the Node Palette
- When selected, Properties Panel shows a warning: "Source IP type 'Temporary' no longer exists"
- The node can still be placed on the canvas (uses cached port configuration)
- Code generation may produce warnings about missing type metadata
