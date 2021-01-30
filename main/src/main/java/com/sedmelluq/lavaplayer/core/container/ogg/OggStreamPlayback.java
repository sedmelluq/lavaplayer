package com.sedmelluq.lavaplayer.core.container.ogg;

import com.sedmelluq.lavaplayer.core.player.playback.AudioPlayback;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackContext;
import com.sedmelluq.lavaplayer.core.player.playback.AudioPlaybackController;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import com.sedmelluq.lavaplayer.core.tools.io.SeekableInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

public class OggStreamPlayback implements AudioPlayback {
  private static final Logger log = LoggerFactory.getLogger(OggStreamPlayback.class);

  private final String identifier;
  private final SeekableInputStream inputStream;

  public OggStreamPlayback(String identifier, SeekableInputStream inputStream) {
    this.identifier = identifier;
    this.inputStream = inputStream;
  }

  @Override
  public void process(AudioPlaybackController controller) {
    OggPacketInputStream packetInputStream = new OggPacketInputStream(inputStream, false);

    log.debug("Starting to play an OGG stream track {}", identifier);

    controller.executeProcessingLoop(() -> {
      try {
        processTrackLoop(packetInputStream, controller.getContext());
      } catch (IOException e) {
        throw new FriendlyException("Stream broke when playing OGG track.", SUSPICIOUS, e);
      }
    }, null, true);
  }

  private void processTrackLoop(OggPacketInputStream packetInputStream, AudioPlaybackContext context) throws IOException, InterruptedException {
    OggTrackBlueprint blueprint = OggTrackLoader.loadTrackBlueprint(packetInputStream);

    if (blueprint == null) {
      throw new IOException("Stream terminated before the first packet.");
    }

    while (blueprint != null) {
      try (OggTrackHandler handler = blueprint.loadTrackHandler(packetInputStream)) {
        handler.initialise(context, 0, 0);
        handler.provideFrames();
      }

      blueprint = OggTrackLoader.loadTrackBlueprint(packetInputStream);
    }
  }
}
