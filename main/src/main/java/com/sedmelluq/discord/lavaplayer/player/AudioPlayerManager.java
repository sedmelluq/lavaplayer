package com.sedmelluq.discord.lavaplayer.player;

import com.sedmelluq.discord.lavaplayer.remote.RemoteNodeRegistry;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.DecodedTrackHolder;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Audio player manager which is used for creating audio players and loading tracks and playlists.
 */
public interface AudioPlayerManager {

  /**
   * Shut down the manager. All threads will be stopped, the manager cannot be used any further. All players created
   * with this manager will stop and all source managers registered to this manager will also be shut down.
   *
   * Every thread created by the audio manager is a daemon thread, so calling this is not required for an application
   * to be able to gracefully shut down, however it should be called if the application continues without requiring this
   * manager any longer.
   */
  void shutdown();

  /**
   * Configure to use remote nodes for playback. On consecutive calls, the connections with previously used nodes will
   * be severed and all remotely playing tracks will be stopped first.
   *
   * @param nodeAddresses The addresses of the remote nodes
   */
  void useRemoteNodes(String... nodeAddresses);

  /**
   * Enable reporting GC pause length statistics to log (warn level with lengths bad for latency, debug level otherwise)
   */
  void enableGcMonitoring();

  /**
   * @param sourceManager The source manager to register, which will be used for subsequent loadItem calls
   */
  void registerSourceManager(AudioSourceManager sourceManager);

  /**
   * Shortcut for accessing a source manager of a certain class.
   * @param klass The class of the source manager to return.
   * @param <T> The class of the source manager.
   * @return The source manager of the specified class, or null if not registered.
   */
  <T extends AudioSourceManager> T source(Class<T> klass);

  /**
   * Schedules loading a track or playlist with the specified identifier.
   * @param identifier    The identifier that a specific source manager should be able to find the track with.
   * @param resultHandler A handler to process the result of this operation. It can either end by finding a track,
   *                      finding a playlist, finding nothing or terminating with an exception.
   * @return A future for this operation
   */
  Future<Void> loadItem(final String identifier, final AudioLoadResultHandler resultHandler);

  /**
   * Schedules loading a track or playlist with the specified identifier with an ordering key so that items with the
   * same ordering key are handled sequentially in the order of calls to this method.
   *
   * @param orderingKey   Object to use as the key for the ordering channel
   * @param identifier    The identifier that a specific source manager should be able to find the track with.
   * @param resultHandler A handler to process the result of this operation. It can either end by finding a track,
   *                      finding a playlist, finding nothing or terminating with an exception.
   * @return A future for this operation
   */
  Future<Void> loadItemOrdered(Object orderingKey, final String identifier, final AudioLoadResultHandler resultHandler);

  /**
   * Encode a track into an output stream. If the decoder is not supposed to know the number of tracks in advance, then
   * the encoder should call MessageOutput#finish() after all the tracks it wanted to write have been written. This will
   * make decodeTrack() return null at that position
   *
   * @param stream The message stream to write it to.
   * @param track The track to encode.
   * @throws IOException On IO error.
   */
  void encodeTrack(MessageOutput stream, AudioTrack track) throws IOException;

  /**
   * Decode a track from an input stream. Null returns value indicates reaching the position where the decoder had
   * called MessageOutput#finish().
   *
   * @param stream The message stream to read it from.
   * @return Holder containing the track if it was successfully decoded.
   * @throws IOException On IO error.
   */
  DecodedTrackHolder decodeTrack(MessageInput stream) throws IOException;

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
   * Sets the number of threads used for loading processing item load requests.
   *
   * @param poolSize Maximum number of concurrent threads used for loading items.
   */
  void setItemLoaderThreadPoolSize(int poolSize);

  /**
   * @return New audio player.
   */
  AudioPlayer createPlayer();

  /**
   * @return Registry of remote nodes currently used.
   */
  RemoteNodeRegistry getRemoteNodeRegistry();

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
