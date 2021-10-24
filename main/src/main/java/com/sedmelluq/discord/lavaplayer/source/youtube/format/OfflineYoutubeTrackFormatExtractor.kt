package com.sedmelluq.discord.lavaplayer.source.youtube.format

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeSignatureResolver
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackFormat
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackJsonData
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface

abstract class OfflineYoutubeTrackFormatExtractor : YoutubeTrackFormatExtractor {
    abstract fun extract(data: YoutubeTrackJsonData): List<YoutubeTrackFormat>

    override fun extract(
        response: YoutubeTrackJsonData,
        httpInterface: HttpInterface,
        signatureResolver: YoutubeSignatureResolver
    ): List<YoutubeTrackFormat> = extract(response)
}
