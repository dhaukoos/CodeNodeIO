/*
 * ContinuousTypes - Type aliases for continuous factory methods
 * Provides type-safe block signatures for generators, sinks, transformers, and filters
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

/**
 * Type alias for continuous generator processing block.
 * Receives an emit function to send values to the output channel.
 *
 * @param T Type of values emitted by the generator
 */
typealias ContinuousGeneratorBlock<T> = suspend (emit: suspend (T) -> Unit) -> Unit

/**
 * Type alias for continuous sink processing block.
 * Receives values from the input channel.
 *
 * @param T Type of values consumed by the sink
 */
typealias ContinuousSinkBlock<T> = suspend (T) -> Unit

/**
 * Type alias for continuous transformer processing block.
 * Transforms input values to output values.
 *
 * @param TIn Type of input values
 * @param TOut Type of output values
 */
typealias ContinuousTransformBlock<TIn, TOut> = suspend (TIn) -> TOut

/**
 * Type alias for continuous filter predicate.
 * Returns true to pass the value through, false to drop it.
 *
 * @param T Type of values being filtered
 */
typealias ContinuousFilterPredicate<T> = suspend (T) -> Boolean
