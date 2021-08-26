package lavaplayer.source

import lavaplayer.container.MediaContainerRegistry
import lavaplayer.manager.AudioPlayerManager
import lavaplayer.source.bandcamp.BandcampItemSourceManager
import lavaplayer.source.getyarn.GetyarnItemSourceManager
import lavaplayer.source.http.HttpItemSourceManager
import lavaplayer.source.local.LocalItemSourceManager
import lavaplayer.source.soundcloud.SoundCloudItemSourceManager
import lavaplayer.source.twitch.TwitchStreamItemSourceManager
import lavaplayer.source.vimeo.VimeoItemSourceManager
import lavaplayer.source.youtube.YoutubeItemSourceManager

/**
 * A helper class for registering built-in source managers to a player manager.
 */
object ItemSourceManagers {
    /**
     * Registers all built-in remote audio sources to the specified player manager. Local file audio source must be
     * registered separately.
     *
     * @param playerManager     Player manager to register the source managers to
     * @param containerRegistry Media container registry to be used by any probing sources.
     */
    @JvmOverloads
    @JvmStatic
    fun registerRemoteSources(
        playerManager: AudioPlayerManager,
        containerRegistry: MediaContainerRegistry? = MediaContainerRegistry.DEFAULT_REGISTRY
    ) {
        playerManager.registerSourceManager(YoutubeItemSourceManager(true))
        playerManager.registerSourceManager(SoundCloudItemSourceManager.createDefault())
        playerManager.registerSourceManager(BandcampItemSourceManager())
        playerManager.registerSourceManager(VimeoItemSourceManager())
        playerManager.registerSourceManager(TwitchStreamItemSourceManager())
        playerManager.registerSourceManager(GetyarnItemSourceManager())
        playerManager.registerSourceManager(HttpItemSourceManager(containerRegistry))
    }

    /**
     * Registers the local file source manager to the specified player manager.
     *
     * @param playerManager Player manager to register the source manager to
     * @param containerRegistry Media container registry to be used by the local source.
     */
    @JvmOverloads
    @JvmStatic
    fun registerLocalSource(
        playerManager: AudioPlayerManager,
        containerRegistry: MediaContainerRegistry? = MediaContainerRegistry.DEFAULT_REGISTRY
    ) {
        playerManager.registerSourceManager(LocalItemSourceManager(containerRegistry))
    }
}
