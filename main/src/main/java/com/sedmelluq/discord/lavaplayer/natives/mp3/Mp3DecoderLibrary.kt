package com.sedmelluq.discord.lavaplayer.natives.mp3

import com.sedmelluq.discord.lavaplayer.natives.ConnectorNativeLibLoader
import java.nio.ByteBuffer
import java.nio.ShortBuffer

internal class Mp3DecoderLibrary private constructor() {
    external fun create(): Long

    external fun destroy(instance: Long)

    external fun decode(
        instance: Long,
        directInput: ByteBuffer?,
        inputLength: Int,
        directOutput: ShortBuffer?,
        outputLengthInBytes: Int
    ): Int

    companion object {
        @kotlin.jvm.JvmStatic
        val instance: Mp3DecoderLibrary
            get() {
                ConnectorNativeLibLoader.loadConnectorLibrary()
                return Mp3DecoderLibrary()
            }
    }
}
