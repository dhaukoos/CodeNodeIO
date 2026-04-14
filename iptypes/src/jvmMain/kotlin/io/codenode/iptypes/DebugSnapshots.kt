/*
 * DebugSnapshots - Custom IP Type
 * @IPType
 * @TypeName DebugSnapshots
 * @TypeId ip_debugsnapshots
 * @Color rgb(158, 158, 158)
 * License: Apache 2.0
 */

package io.codenode.iptypes

data class DebugSnapshots(
    val connectionId: String,
    val timestamp: String,
    val value: String
)
