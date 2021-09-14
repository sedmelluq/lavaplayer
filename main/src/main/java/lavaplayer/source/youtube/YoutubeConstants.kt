package lavaplayer.source.youtube

object YoutubeConstants {
    // YouTube constants
    internal const val YOUTUBE_ORIGIN = "https://www.youtube.com"
    internal const val BASE_URL = "$YOUTUBE_ORIGIN/youtubei/v1"
    internal const val INNERTUBE_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    internal const val CLIENT_NAME = "ANDROID"
    internal const val CLIENT_VERSION = "16.24"
    internal const val BASE_PAYLOAD =
        "{\"context\":{\"client\":{\"clientName\":\"$CLIENT_NAME\",\"clientVersion\":\"$CLIENT_VERSION\"}},"
    internal const val PLAYER_URL = "$BASE_URL/player?key=$INNERTUBE_API_KEY"
    internal const val PLAYER_PAYLOAD =
        "$BASE_PAYLOAD\"racyCheckOk\":true,\"contentCheckOk\":true,\"videoId\":\"%s\",\"playbackContext\":{\"contentPlaybackContext\":{\"signatureTimestamp\":%s}}}"
    internal const val VERIFY_AGE_URL = "$BASE_URL/verify_age?key=$INNERTUBE_API_KEY"
    internal const val VERIFY_AGE_PAYLOAD =
        "$BASE_PAYLOAD\"nextEndpoint\":{\"urlEndpoint\":{\"url\":\"%s\"}},\"setControvercy\":true}"
    internal const val SEARCH_URL = "$BASE_URL/search?key=$INNERTUBE_API_KEY"
    internal const val SEARCH_PAYLOAD = "$BASE_PAYLOAD\"query\":\"%s\",\"params\":\"EgIQAQ==\"}"
    internal const val BROWSE_URL = "$BASE_URL/browse?key=$INNERTUBE_API_KEY"
    internal const val BROWSE_CONTINUATION_PAYLOAD = "$BASE_PAYLOAD\"continuation\":\"%s\"}"
    internal const val BROWSE_PLAYLIST_PAYLOAD = "$BASE_PAYLOAD\"browseId\":\"VL%s\"}"
    internal const val NEXT_URL = "$BASE_URL/next?key=$INNERTUBE_API_KEY"
    internal const val NEXT_PAYLOAD = "$BASE_PAYLOAD\"videoId\":\"%s\",\"playlistId\":\"%s\"}"

    // YouTube Music constants
    internal const val MUSIC_BASE_URL = "https://music.youtube.com/youtubei/v1"
    internal const val MUSIC_INNERTUBE_API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
    internal const val MUSIC_CLIENT_NAME = "WEB_REMIX"
    internal const val MUSIC_CLIENT_VERSION = "0.1"
    internal const val MUSIC_BASE_PAYLOAD =
        "{\"context\":{\"client\":{\"clientName\":\"$MUSIC_CLIENT_NAME\",\"clientVersion\":\"$MUSIC_CLIENT_VERSION\"}},"
    internal const val MUSIC_SEARCH_URL = "$MUSIC_BASE_URL/search?key=$MUSIC_INNERTUBE_API_KEY"
    internal const val MUSIC_SEARCH_PAYLOAD =
        "$MUSIC_BASE_PAYLOAD\"query\":\"%s\",\"params\":\"Eg-KAQwIARAAGAAgACgAMABqChADEAQQCRAFEAo=\"}"
    internal const val WATCH_URL_PREFIX = "$YOUTUBE_ORIGIN/watch?v="
}
