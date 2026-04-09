/*
 * BaseState - Marker Interface for ViewModel State Classes
 * All ViewModel state data classes should implement this interface
 * License: Apache 2.0
 */

package io.codenode.flowgraphcompose.viewmodel

/**
 * Marker interface for ViewModel state data classes.
 *
 * All state data classes used with ViewModels should implement this interface
 * to enable type-safe state handling and provide a common contract.
 *
 * State classes implementing BaseState should:
 * - Be immutable data classes
 * - Use copy() for state updates
 * - Contain only serializable properties when persistence is needed
 */
interface BaseState
