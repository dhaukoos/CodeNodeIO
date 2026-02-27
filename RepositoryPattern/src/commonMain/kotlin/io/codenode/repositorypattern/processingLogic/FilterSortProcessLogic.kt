package io.codenode.repositorypattern.processingLogic

import io.codenode.fbpdsl.runtime.In3Out1TickBlock
import io.codenode.repositorypattern.stateProperties.FilterSortStateProperties

/**
 * Tick function for the FilterSort node.
 *
 * Node type: Processor (3 inputs, 1 outputs)
 *
 * Inputs:
 *   - input1: Any
 *   - input2: Any
 *   - input3: Any
 *
 * Outputs:
 *   - output1: Any
 *
 */
val filterSortTick: In3Out1TickBlock<Any, Any, Any, Any> = { input1, input2, input3 ->
    // TODO: Implement FilterSort tick logic
    TODO("Provide default value")
}
