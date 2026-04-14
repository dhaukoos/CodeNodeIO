/*
 * IPTypeCommand - Custom IP Type
 * @IPType
 * @TypeName IPTypeCommand
 * @TypeId ip_iptypecommand
 * @Color rgb(244, 67, 54)
 * License: Apache 2.0
 */

package io.codenode.iptypes

data class IPTypeCommand(
    val command: String,
    val targetTypeId: String,
    val payload: String
)
