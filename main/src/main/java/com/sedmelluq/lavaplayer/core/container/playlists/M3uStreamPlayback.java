package com.sedmelluq.lavaplayer.core.container.playlists;

import com.sedmelluq.lavaplayer.core.tools.io.ChainedInputStream;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import java.io.InputStream;

/**
 * Audio track that handles processing M3U segment streams which using MPEG-TS wrapped ADTS codec.
 */
public abstract class M3uStreamPlayback implements AudioPlayback {
  protected abstract M3uStreamSegmentUrlProvider getSegmentUrlProvider();

  protected abstract HttpInterface getHttpInterface();

  protected abstract void processJoinedStream(
      AudioPlaybackController controller,
      InputStream stream
  ) throws Exception;

  @Override
  public void process(AudioPlaybackController controller) {
    try (
        HttpInterface httpInterface = getHttpInterface();
        ChainedInputStream chainedInputStream = new ChainedInputStream(() ->
            getSegmentUrlProvider().getNextSegmentStream(httpInterface))
    ) {
      processJoinedStream(controller, chainedInputStream);
    } catch (Exception e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }
}
