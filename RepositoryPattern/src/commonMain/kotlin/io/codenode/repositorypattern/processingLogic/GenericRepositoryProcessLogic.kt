package io.codenode.repositorypattern.processingLogic

import io.codenode.fbpdsl.runtime.In3Out1TickBlock

/**
 * Tick function for the GenericRepository node.
 *
 * Node type: Processor (3 inputs, 1 outputs)
 *
 * Inputs:
 *   - insert: Any
 *   - update: Any
 *   - delete: Any
 *
 * Outputs:
 *   - getAll: Any
 *
 */
val genericRepositoryTick: In3Out1TickBlock<Any, Any, Any, Any> = { insert, update, delete ->
    // TODO: Implement GenericRepository tick logic
    TODO("Provide default value")
}
