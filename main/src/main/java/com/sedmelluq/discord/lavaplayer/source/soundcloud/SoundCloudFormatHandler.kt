package com.sedmelluq.discord.lavaplayer.source.soundcloud

interface SoundCloudFormatHandler {
    fun chooseBestFormat(formats: List<SoundCloudTrackFormat>): SoundCloudTrackFormat

    fun buildFormatIdentifier(format: SoundCloudTrackFormat): String

    fun getM3uInfo(identifier: String): SoundCloudM3uInfo?

    fun getMp3LookupUrl(identifier: String): String?
}
