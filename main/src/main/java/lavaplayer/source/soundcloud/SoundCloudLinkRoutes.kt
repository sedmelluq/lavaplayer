package lavaplayer.source.soundcloud

import lavaplayer.source.common.LinkRoutes
import lavaplayer.track.AudioItem

interface SoundCloudLinkRoutes : LinkRoutes {
    fun track(url: String): AudioItem?

    fun liked(url: String): AudioItem?

    fun set(url: String): AudioItem?

    fun search(query: String, offset: Int, limit: Int): AudioItem?
}
