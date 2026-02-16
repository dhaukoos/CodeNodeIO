/*
 * ProcessResult - Multi-output result containers for typed node runtimes
 * Provides nullable fields for selective output sending
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

/**
 * Multi-output result container for 2 outputs.
 *
 * Used by nodes with 2 output channels to return nullable values,
 * enabling selective sending (only non-null values are sent).
 *
 * @param U Type of first output
 * @param V Type of second output
 * @property out1 First output value, nullable for selective sending
 * @property out2 Second output value, nullable for selective sending
 */
data class ProcessResult2<U, V>(
    val out1: U?,
    val out2: V?
) {
    companion object {
        /**
         * Creates a ProcessResult2 with explicit nullable values.
         */
        fun <U, V> of(out1: U?, out2: V?) = ProcessResult2(out1, out2)

        /**
         * Creates a ProcessResult2 with only the first output.
         */
        fun <U, V> first(value: U) = ProcessResult2<U, V>(value, null)

        /**
         * Creates a ProcessResult2 with only the second output.
         */
        fun <U, V> second(value: V) = ProcessResult2<U, V>(null, value)

        /**
         * Creates a ProcessResult2 with both outputs.
         */
        fun <U, V> both(first: U, second: V) = ProcessResult2(first, second)
    }
}

/**
 * Multi-output result container for 3 outputs.
 *
 * Used by nodes with 3 output channels to return nullable values,
 * enabling selective sending (only non-null values are sent).
 *
 * @param U Type of first output
 * @param V Type of second output
 * @param W Type of third output
 * @property out1 First output value, nullable for selective sending
 * @property out2 Second output value, nullable for selective sending
 * @property out3 Third output value, nullable for selective sending
 */
data class ProcessResult3<U, V, W>(
    val out1: U?,
    val out2: V?,
    val out3: W?
) {
    companion object {
        /**
         * Creates a ProcessResult3 with explicit nullable values.
         */
        fun <U, V, W> of(out1: U?, out2: V?, out3: W?) = ProcessResult3(out1, out2, out3)

        /**
         * Creates a ProcessResult3 with all three outputs.
         */
        fun <U, V, W> all(first: U, second: V, third: W) = ProcessResult3(first, second, third)

        /**
         * Creates a ProcessResult3 with only the first output.
         */
        fun <U, V, W> first(value: U) = ProcessResult3<U, V, W>(value, null, null)

        /**
         * Creates a ProcessResult3 with only the second output.
         */
        fun <U, V, W> second(value: V) = ProcessResult3<U, V, W>(null, value, null)

        /**
         * Creates a ProcessResult3 with only the third output.
         */
        fun <U, V, W> third(value: W) = ProcessResult3<U, V, W>(null, null, value)
    }
}
