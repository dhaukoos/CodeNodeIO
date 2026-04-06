/*
 * IPTypeFileMeta - Metadata extracted from an IP type definition file on the filesystem
 * License: Apache 2.0
 */

package io.codenode.grapheditor.model

import io.codenode.fbpdsl.model.IPColor
import io.codenode.fbpdsl.model.PlacementLevel

/**
 * Metadata extracted from a `.kt` IP type definition file during filesystem discovery.
 * This is the discovery-time representation used before the type is registered in IPTypeRegistry.
 *
 * @property typeName Display name (e.g., "Coordinates")
 * @property typeId Unique identifier (e.g., "ip_coordinates")
 * @property color Palette display color
 * @property properties Parsed data class fields
 * @property filePath Absolute path to the `.kt` source file
 * @property tier Which filesystem tier this type was discovered from
 * @property packageName Kotlin package declaration from the file
 * @property className Fully qualified class name (package + typeName)
 */
data class IPTypeFileMeta(
    val typeName: String,
    val typeId: String,
    val color: IPColor,
    val properties: List<IPPropertyMeta>,
    val filePath: String,
    val tier: PlacementLevel,
    val packageName: String,
    val className: String
)
