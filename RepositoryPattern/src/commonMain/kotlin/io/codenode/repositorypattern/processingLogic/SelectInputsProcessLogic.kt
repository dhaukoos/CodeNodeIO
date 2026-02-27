package io.codenode.repositorypattern.processingLogic

import io.codenode.fbpdsl.runtime.Out2TickBlock
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.repositorypattern.stateProperties.SelectInputsStateProperties

/**
 * Tick function for the SelectInputs node.
 *
 * Node type: Generator (0 inputs, 2 outputs)
 *
 * Outputs:
 *   - filterBy: Any
 *   - sortBy: Any
 *
 */
val selectInputsTick: Out2TickBlock<Any, Any> = {
    // TODO: Implement SelectInputs tick logic
    ProcessResult2.both(TODO("Provide default value"), TODO("Provide default value"))
}
