package lavaplayer.source;

import lavaplayer.container.MediaContainerRegistry;
import lavaplayer.manager.AudioPlayerManager;
import lavaplayer.source.bandcamp.BandcampItemSourceManager;
import lavaplayer.source.getyarn.GetyarnItemSourceManager;
import lavaplayer.source.http.HttpItemSourceManager;
import lavaplayer.source.local.LocalItemSourceManager;
import lavaplayer.source.soundcloud.SoundCloudItemSourceManager;
import lavaplayer.source.twitch.TwitchStreamItemSourceManager;
import lavaplayer.source.vimeo.VimeoItemSourceManager;
import lavaplayer.source.youtube.YoutubeItemSourceManager;

/**
 * A helper class for registering built-in source managers to a player manager.
 */
public class ItemSourceManagers {
    /**
     * See {@link #registerRemoteSources(AudioPlayerManager, MediaContainerRegistry)}, but with default containers.
     */
    public static void registerRemoteSources(AudioPlayerManager playerManager) {
        registerRemoteSources(playerManager, MediaContainerRegistry.DEFAULT_REGISTRY);
    }

    /**
     * Registers all built-in remote audio sources to the specified player manager. Local file audio source must be
     * registered separately.
     *
     * @param playerManager     Player manager to register the source managers to
     * @param containerRegistry Media container registry to be used by any probing sources.
     */
    public static void registerRemoteSources(AudioPlayerManager playerManager, MediaContainerRegistry containerRegistry) {
        playerManager.registerSourceManager(new YoutubeItemSourceManager(true));
        playerManager.registerSourceManager(SoundCloudItemSourceManager.createDefault());
        playerManager.registerSourceManager(new BandcampItemSourceManager());
        playerManager.registerSourceManager(new VimeoItemSourceManager());
        playerManager.registerSourceManager(new TwitchStreamItemSourceManager());
        playerManager.registerSourceManager(new GetyarnItemSourceManager());
        playerManager.registerSourceManager(new HttpItemSourceManager(containerRegistry));
    }

    /**
     * Registers the local file source manager to the specified player manager.
     *
     * @param playerManager Player manager to register the source manager to
     */
    public static void registerLocalSource(AudioPlayerManager playerManager) {
        registerLocalSource(playerManager, MediaContainerRegistry.DEFAULT_REGISTRY);
    }

    /**
     * Registers the local file source manager to the specified player manager.
     *
     * @param playerManager     Player manager to register the source manager to
     * @param containerRegistry Media container registry to be used by the local source.
     */
    public static void registerLocalSource(AudioPlayerManager playerManager, MediaContainerRegistry containerRegistry) {
        playerManager.registerSourceManager(new LocalItemSourceManager(containerRegistry));
    }
}
