/*
 * BaseState - Marker Interface for ViewModel State Classes
 * All ViewModel state data classes should implement this interface
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.viewmodel

/**
 * Marker interface for ViewModel state data classes.
 *
 * State classes implementing BaseState should:
 * - Be immutable data classes
 * - Use copy() for state updates
 * - Contain only serializable properties when persistence is needed
 */
interface BaseState
