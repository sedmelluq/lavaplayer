package com.sedmelluq.lavaplayer.core.info.loader;

import com.sedmelluq.lavaplayer.core.info.playlist.AudioPlaylist;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import java.util.function.Consumer;

/**
 * Helper class for creating an audio result handler using only methods that can be passed as lambdas.
 */
public class SplitAudioInfoResponseHandler implements AudioInfoResponseHandler {
  private final Consumer<AudioTrackInfo> trackConsumer;
  private final Consumer<AudioPlaylist> playlistConsumer;
  private final Runnable emptyResultHandler;
  private final Consumer<FriendlyException> exceptionConsumer;

  /**
   * Refer to {@link AudioInfoResponseHandler} methods for details on when each method is called.
   *
   * @param trackConsumer Consumer for single track result
   * @param playlistConsumer Consumer for playlist result
   * @param emptyResultHandler Empty result handler
   * @param exceptionConsumer Consumer for an exception when loading the item fails
   */
  public SplitAudioInfoResponseHandler(
      Consumer<AudioTrackInfo> trackConsumer,
      Consumer<AudioPlaylist> playlistConsumer,
      Runnable emptyResultHandler,
      Consumer<FriendlyException> exceptionConsumer
  ) {
    this.trackConsumer = trackConsumer;
    this.playlistConsumer = playlistConsumer;
    this.emptyResultHandler = emptyResultHandler;
    this.exceptionConsumer = exceptionConsumer;
  }

  @Override
  public void trackLoaded(AudioTrackInfo track) {
    if (trackConsumer != null) {
      trackConsumer.accept(track);
    }
  }

  @Override
  public void playlistLoaded(AudioPlaylist playlist) {
    if (playlistConsumer != null) {
      playlistConsumer.accept(playlist);
    }
  }

  @Override
  public void noMatches() {
    if (emptyResultHandler != null) {
      emptyResultHandler.run();
    }
  }

  @Override
  public void loadFailed(FriendlyException exception) {
    if (exceptionConsumer != null) {
      exceptionConsumer.accept(exception);
    }
  }
}
