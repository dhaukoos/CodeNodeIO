/*
 * ContinuousTypes - Type aliases for continuous factory methods
 * Provides type-safe block signatures for generators, sinks, transformers, and filters
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import kotlinx.coroutines.channels.Channel

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

/**
 * Creates a typed channel for wiring continuous nodes together.
 *
 * This is a convenience function for creating channels with appropriate
 * capacity for FBP-style data flow between nodes.
 *
 * @param T Type of values flowing through the channel
 * @param capacity Channel buffer capacity (default: BUFFERED = 64)
 * @return A new Channel instance
 */
fun <T> createFlowChannel(capacity: Int = Channel.BUFFERED): Channel<T> = Channel(capacity)

// ========== Multi-Input, Single Output Process Blocks ==========

/**
 * Type alias for 2-input, 1-output processor block.
 * Receives two values and produces one output.
 *
 * @param A Type of first input
 * @param B Type of second input
 * @param R Type of output
 */
typealias In2Out1ProcessBlock<A, B, R> = suspend (A, B) -> R

/**
 * Type alias for 3-input, 1-output processor block.
 * Receives three values and produces one output.
 *
 * @param A Type of first input
 * @param B Type of second input
 * @param C Type of third input
 * @param R Type of output
 */
typealias In3Out1ProcessBlock<A, B, C, R> = suspend (A, B, C) -> R

// ========== Single Input, Multi-Output Process Blocks ==========

/**
 * Type alias for 1-input, 2-output processor block.
 * Receives one value and produces ProcessResult2.
 *
 * @param A Type of input
 * @param U Type of first output
 * @param V Type of second output
 */
typealias In1Out2ProcessBlock<A, U, V> = suspend (A) -> ProcessResult2<U, V>

/**
 * Type alias for 1-input, 3-output processor block.
 * Receives one value and produces ProcessResult3.
 *
 * @param A Type of input
 * @param U Type of first output
 * @param V Type of second output
 * @param W Type of third output
 */
typealias In1Out3ProcessBlock<A, U, V, W> = suspend (A) -> ProcessResult3<U, V, W>

// ========== Multi-Input, Multi-Output Process Blocks ==========

/**
 * Type alias for 2-input, 2-output processor block.
 * Receives two values and produces ProcessResult2.
 *
 * @param A Type of first input
 * @param B Type of second input
 * @param U Type of first output
 * @param V Type of second output
 */
typealias In2Out2ProcessBlock<A, B, U, V> = suspend (A, B) -> ProcessResult2<U, V>

/**
 * Type alias for 2-input, 3-output processor block.
 * Receives two values and produces ProcessResult3.
 *
 * @param A Type of first input
 * @param B Type of second input
 * @param U Type of first output
 * @param V Type of second output
 * @param W Type of third output
 */
typealias In2Out3ProcessBlock<A, B, U, V, W> = suspend (A, B) -> ProcessResult3<U, V, W>

/**
 * Type alias for 3-input, 2-output processor block.
 * Receives three values and produces ProcessResult2.
 *
 * @param A Type of first input
 * @param B Type of second input
 * @param C Type of third input
 * @param U Type of first output
 * @param V Type of second output
 */
typealias In3Out2ProcessBlock<A, B, C, U, V> = suspend (A, B, C) -> ProcessResult2<U, V>

/**
 * Type alias for 3-input, 3-output processor block.
 * Receives three values and produces ProcessResult3.
 *
 * @param A Type of first input
 * @param B Type of second input
 * @param C Type of third input
 * @param U Type of first output
 * @param V Type of second output
 * @param W Type of third output
 */
typealias In3Out3ProcessBlock<A, B, C, U, V, W> = suspend (A, B, C) -> ProcessResult3<U, V, W>

// ========== Multi-Input Sink Blocks ==========

/**
 * Type alias for 2-input sink block.
 * Consumes two values with no output.
 *
 * @param A Type of first input
 * @param B Type of second input
 */
typealias In2SinkBlock<A, B> = suspend (A, B) -> Unit

/**
 * Type alias for 3-input sink block.
 * Consumes three values with no output.
 *
 * @param A Type of first input
 * @param B Type of second input
 * @param C Type of third input
 */
typealias In3SinkBlock<A, B, C> = suspend (A, B, C) -> Unit

// ========== Multi-Output Generator Blocks ==========

/**
 * Type alias for 2-output generator block.
 * Emits ProcessResult2 values to two output channels.
 *
 * @param U Type of first output
 * @param V Type of second output
 */
typealias Out2GeneratorBlock<U, V> = suspend (emit: suspend (ProcessResult2<U, V>) -> Unit) -> Unit

/**
 * Type alias for 2-output timed tick block.
 * Called once per tick interval by the runtime, returns values to emit.
 *
 * @param U Type of first output
 * @param V Type of second output
 */
typealias Out2TickBlock<U, V> = suspend () -> ProcessResult2<U, V>

/**
 * Type alias for 3-output generator block.
 * Emits ProcessResult3 values to three output channels.
 *
 * @param U Type of first output
 * @param V Type of second output
 * @param W Type of third output
 */
typealias Out3GeneratorBlock<U, V, W> = suspend (emit: suspend (ProcessResult3<U, V, W>) -> Unit) -> Unit
