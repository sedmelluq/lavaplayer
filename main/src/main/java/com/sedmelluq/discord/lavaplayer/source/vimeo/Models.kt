package com.sedmelluq.discord.lavaplayer.source.vimeo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
internal data class VimeoClipPage(val clip: VimeoClip, val owner: Owner, val thumbnail: Thumbnail, val player: Player) {
    @Serializable
    data class Owner(@SerialName("display_name") val displayName: String)

    @Serializable
    data class Thumbnail(val src: String)

    @Serializable
    data class Player(@SerialName("config_url") val configUrl: String)
}

@Serializable
internal data class VimeoPlayer(val request: Request) {
    @Serializable
    data class Request(val files: Files)

    @Serializable
    data class Files(val progressive: List<Progressive>) {
        val best: Progressive
            get() {
                val sums = progressive.map { file -> file.fps + file.quality.dropLast(1).toInt() }
                val sumsAvg = sums.fold(0) { acc, sum -> acc + sum } / sums.size
                return progressive[sums.indexOf(sums.closestValue(sumsAvg))]
            }

        private fun List<Int>.closestValue(value: Int) = minByOrNull { abs(value - it) }

        @Serializable
        data class Progressive(val quality: String, val fps: Int, val url: String)
    }
}

@Serializable
internal data class VimeoClip(val title: String, val duration: Duration) {
    @Serializable
    data class Duration(val raw: Double)
}
