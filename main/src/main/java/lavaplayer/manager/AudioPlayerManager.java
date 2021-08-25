package lavaplayer.manager;

import lavaplayer.source.Sources;
import lavaplayer.track.TrackEncoder;
import lavaplayer.track.loading.ItemLoaderFactory;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Audio player manager which is used for creating audio players and loading tracks and playlists.
 */
public interface AudioPlayerManager extends TrackEncoder, Sources {

    /**
     * Shut down the manager. All threads will be stopped, the manager cannot be used any further. All players created
     * with this manager will stop and all source managers registered to this manager will also be shut down.
     * <p>
     * Every thread created by the audio manager is a daemon thread, so calling this is not required for an application
     * to be able to gracefully shut down, however it should be called if the application continues without requiring this
     * manager any longer.
     */
    void shutdown();

    /**
     * Enable reporting GC pause length statistics to log (warn level with lengths bad for latency, debug level otherwise)
     */
    void enableGcMonitoring();

    /**
     * @return Audio processing configuration used for tracks executed by this manager.
     */
    AudioConfiguration getConfiguration();

    /**
     * Seek ghosting is the effect where while a seek is in progress, buffered audio from the previous location will be
     * served until seek is ready or the buffer is empty.
     *
     * @return True if seek ghosting is enabled.
     */
    boolean isUsingSeekGhosting();

    /**
     * @param useSeekGhosting The new state of seek ghosting
     */
    void setUseSeekGhosting(boolean useSeekGhosting);

    /**
     * @return The item loader factory for this player manager.
     */
    ItemLoaderFactory getItemLoaders();

    /**
     * @return The length of the internal buffer for audio in milliseconds.
     */
    int getFrameBufferDuration();

    /**
     * @param frameBufferDuration New length of the internal buffer for audio in milliseconds.
     */
    void setFrameBufferDuration(int frameBufferDuration);

    /**
     * Sets the threshold for how long a track can be stuck until the TrackStuckEvent is sent out. A track is considered
     * to be stuck if the player receives requests for audio samples from the track, but the audio frame provider of that
     * track has been returning no data for the specified time.
     *
     * @param trackStuckThreshold The threshold in milliseconds.
     */
    void setTrackStuckThreshold(long trackStuckThreshold);

    /**
     * Sets the threshold for clearing an audio player when it has not been queried for the specified amount of time.
     *
     * @param cleanupThreshold The threshold in milliseconds.
     */
    void setPlayerCleanupThreshold(long cleanupThreshold);

    /**
     * @return New audio player.
     */
    AudioPlayer createPlayer();

    /**
     * @param configurator Function used to reconfigure the request config of all sources which perform HTTP requests.
     *                     Applied to all current and future registered sources. Setting this while sources are already in
     *                     use will close all active connections, so this should be called before the sources have been
     *                     used.
     */
    void setHttpRequestConfigurator(Function<RequestConfig, RequestConfig> configurator);

    /**
     * @param configurator Function used to reconfigure the HTTP builder of all sources which perform HTTP requests.
     *                     Applied to all current and future registered sources. Setting this while sources are already in
     *                     use will close all active connections, so this should be called before the sources have been
     *                     used.
     */
    void setHttpBuilderConfigurator(Consumer<HttpClientBuilder> configurator);

}
