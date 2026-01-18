/*
 * Connection - Temporary stub for compilation
 * Will be properly implemented in T019
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.serialization.Serializable

/**
 * Temporary stub for Connection class
 * TODO: Properly implement in task T019
 */
@Serializable
data class Connection(
    val id: String,
    val sourceNodeId: String,
    val sourcePortId: String,
    val targetNodeId: String,
    val targetPortId: String
)
