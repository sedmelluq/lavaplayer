package lavaplayer.source.common

import lavaplayer.container.MediaContainerRegistry
import lavaplayer.source.ItemSourceManager
import lavaplayer.source.bandcamp.BandcampItemSourceManager
import lavaplayer.source.getyarn.GetyarnItemSourceManager
import lavaplayer.source.http.HttpItemSourceManager
import lavaplayer.source.local.LocalItemSourceManager
import lavaplayer.source.soundcloud.SoundCloudItemSourceManager
import lavaplayer.source.twitch.TwitchStreamItemSourceManager
import lavaplayer.source.vimeo.VimeoItemSourceManager
import lavaplayer.source.youtube.YoutubeItemSourceManager

interface SourceRegistry {
    /**
     * Helpers for registering built-in source managers to a player manager.
     */
    companion object {
        /**
         * Registers all built-in remote audio sources to the specified player manager. Local file audio source must be
         * registered separately.
         *
         * @param sourceRegistry    Source registry to register the source managers to
         * @param containerRegistry Media container registry to be used by any probing sources.
         */
        @JvmOverloads
        @JvmStatic
        fun registerRemoteSources(
            sourceRegistry: SourceRegistry,
            containerRegistry: MediaContainerRegistry = MediaContainerRegistry.DEFAULT_REGISTRY
        ) {
            sourceRegistry.registerSourceManager(YoutubeItemSourceManager(true))
            sourceRegistry.registerSourceManager(SoundCloudItemSourceManager.createDefault())
            sourceRegistry.registerSourceManager(BandcampItemSourceManager())
            sourceRegistry.registerSourceManager(VimeoItemSourceManager())
            sourceRegistry.registerSourceManager(TwitchStreamItemSourceManager())
            sourceRegistry.registerSourceManager(GetyarnItemSourceManager())
            sourceRegistry.registerSourceManager(HttpItemSourceManager(containerRegistry))
        }

        /**
         * Registers the local file source manager to the specified player manager.
         *
         * @param sourceRegistry    Source registry to register the source manager to
         * @param containerRegistry Media container registry to be used by the local source.
         */
        @JvmOverloads
        @JvmStatic
        fun registerLocalSource(
            sourceRegistry: SourceRegistry,
            containerRegistry: MediaContainerRegistry = MediaContainerRegistry.DEFAULT_REGISTRY
        ) {
            sourceRegistry.registerSourceManager(LocalItemSourceManager(containerRegistry))
        }
    }

    /**
     * The list of enabled source managers.
     */
    val sourceManagers: List<ItemSourceManager>

    /**
     * Registers an [ItemSourceManager]
     * @param sourceManager The source manager to register, which will be used for subsequent load item calls.
     */
    fun registerSourceManager(sourceManager: ItemSourceManager)

    /**
     * Shortcut for accessing a source manager of the specified class.
     *
     * @param klass The class of the source manager to return
     * @param T     The class of the source manager.
     *
     * @return The source manager of the specified class, or null if not registered.
     */
    fun <T : ItemSourceManager> source(klass: Class<T>): T?
}
