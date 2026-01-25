package io.codenode.fbpdsl.model

import kotlin.reflect.KClass

/**
 * Serializer for KClass to handle serialization across platforms
 * This is a placeholder - actual implementation would need platform-specific handling
 */
object KClassSerializer : kotlinx.serialization.KSerializer<KClass<*>> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
        "KClass",
        kotlinx.serialization.descriptors.PrimitiveKind.STRING
    )

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: KClass<*>) {
        encoder.encodeString(value.simpleName ?: value.toString())
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): KClass<*> {
        // This is a simplified implementation
        // In production, you'd need a proper class registry or reflection mechanism
        return Any::class
    }
}
