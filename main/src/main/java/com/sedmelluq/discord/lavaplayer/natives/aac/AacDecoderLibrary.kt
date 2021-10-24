package com.sedmelluq.discord.lavaplayer.natives.aac

import com.sedmelluq.discord.lavaplayer.natives.ConnectorNativeLibLoader.loadConnectorLibrary
import java.nio.ByteBuffer
import java.nio.ShortBuffer

internal class AacDecoderLibrary private constructor() {
    external fun create(transportType: Int): Long

    external fun destroy(instance: Long)

    external fun configure(instance: Long, bufferData: Long): Int

    external fun fill(instance: Long, directBuffer: ByteBuffer?, offset: Int, length: Int): Int

    external fun decode(instance: Long, directBuffer: ShortBuffer?, length: Int, flush: Boolean): Int

    external fun getStreamInfo(instance: Long): Long

    companion object {
        @kotlin.jvm.JvmStatic
        val instance: AacDecoderLibrary
            get() {
                loadConnectorLibrary()
                return AacDecoderLibrary()
            }
    }
}
