package com.sedmelluq.discord.lavaplayer.tools

object ThumbnailTools {

    private const val YOUTUBE_THUMBNAIL_FORMAT: String = "https://img.youtube.com/vi/%s/0.jpg"

    @JvmStatic
    fun extractYouTube(jsonBrowser: JsonBrowser, videoId: String): String {
        val thumbnails = jsonBrowser["thumbnail"]["thumbnails"].values()
        val thumbnail = thumbnails
            .maxByOrNull { it["width"].asLong(0) + it["height"].asLong(0) }
            ?: return YOUTUBE_THUMBNAIL_FORMAT.format(videoId)

        return thumbnail["url"].safeText
    }

    @JvmStatic
    fun extractYouTubeMusic(jsonBrowser: JsonBrowser, videoId: String): String {
        val thumbnails = jsonBrowser["musicResponsiveListItemRenderer"]["thumbnail"]["musicThumbnailRenderer"]["thumbnail"]["thumbnails"].values()
        return thumbnails.maxByOrNull { it["width"].asLong(0) * it["height"].asLong(0) }
            ?.get("url")?.safeText
            ?: "https://img.youtube.com/vi/$videoId/0.jpg"
    }


    @JvmStatic
    fun extractSoundCloud(jsonBrowser: JsonBrowser): String {
        val thumbnail = jsonBrowser["artwork_url"]
        return if (!thumbnail.isNull) {
            thumbnail.safeText
        } else {
            jsonBrowser["user"]["avatar_url"].safeText.replace("large.jpg", "original.jpg")
        }
    }

}
