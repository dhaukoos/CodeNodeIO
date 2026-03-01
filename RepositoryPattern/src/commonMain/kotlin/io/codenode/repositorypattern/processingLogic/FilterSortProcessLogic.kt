package io.codenode.repositorypattern.processingLogic

import io.codenode.fbpdsl.runtime.In3Out1TickBlock

/**
 * Tick function for the FilterSort node.
 *
 * Node type: Processor (3 inputs, 1 outputs)
 *
 * Inputs:
 *   - filterBy: Any
 *   - sortBy: Any
 *   - observeAll: Any
 *
 * Outputs:
 *   - filteredList: Any
 *
 */
val filterSortTick: In3Out1TickBlock<Any, Any, Any, Any> = { filterBy, sortBy, observeAll ->
    // TODO: Implement FilterSort tick logic
    TODO("Provide default value")
}
