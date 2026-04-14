/*
 * EditorGraphState - Custom IP Type
 * @IPType
 * @TypeName EditorGraphState
 * @TypeId ip_editorgraphstate
 * @Color rgb(103, 58, 183)
 * License: Apache 2.0
 */

package io.codenode.iptypes

data class EditorGraphState(
    val flowGraph: String,
    val selection: String,
    val panOffset: String,
    val scale: String,
    val isDirty: Boolean = false
)
