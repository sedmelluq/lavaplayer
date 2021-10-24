package com.sedmelluq.discord.lavaplayer.source.youtube.format

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeSignatureResolver
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackFormat
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackJsonData
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface

interface YoutubeTrackFormatExtractor {
    companion object {
        const val DEFAULT_SIGNATURE_KEY = "signature"
    }

    fun extract(
        response: YoutubeTrackJsonData,
        httpInterface: HttpInterface,
        signatureResolver: YoutubeSignatureResolver
    ): List<YoutubeTrackFormat>
}
