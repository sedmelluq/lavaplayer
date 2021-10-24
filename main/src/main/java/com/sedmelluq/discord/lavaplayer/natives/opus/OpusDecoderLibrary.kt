package com.sedmelluq.discord.lavaplayer.natives.opus

import com.sedmelluq.discord.lavaplayer.natives.ConnectorNativeLibLoader.loadConnectorLibrary
import java.nio.ByteBuffer
import java.nio.ShortBuffer

internal class OpusDecoderLibrary private constructor() {
    external fun create(sampleRate: Int, channels: Int): Long
    external fun destroy(instance: Long)
    external fun decode(
        instance: Long,
        directInput: ByteBuffer?,
        inputSize: Int,
        directOutput: ShortBuffer?,
        frameSize: Int
    ): Int

    companion object {
        @JvmStatic
        val instance: OpusDecoderLibrary
            get() {
                loadConnectorLibrary()
                return OpusDecoderLibrary()
            }
    }
}
