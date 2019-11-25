package com.sedmelluq.discord.lavaplayer.container.ogg;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Audio track which handles an OGG stream.
 */
public class OggAudioTrack extends BaseAudioTrack {
  private static final Logger log = LoggerFactory.getLogger(OggAudioTrack.class);

  private final SeekableInputStream inputStream;
  private final Persistent persistent;

  /**
   * @param trackInfo Track info
   * @param inputStream Input stream for the OGG stream
   */
  public OggAudioTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream, Persistent persistent) {
    super(trackInfo);

    this.inputStream = inputStream;
    this.persistent = persistent;
  }

  @Override
  public void process(final LocalAudioTrackExecutor localExecutor) {
    OggPacketInputStream packetInputStream = new OggPacketInputStream(inputStream);

    log.debug("Starting to play an OGG stream track {}", getIdentifier());

    localExecutor.executeProcessingLoop(() -> {
      try {
        if (persistent == null) {
          processTrackLoop(packetInputStream, localExecutor.getProcessingContext());
        } else {
          processPersistent(packetInputStream, localExecutor.getProcessingContext());
        }
      } catch (IOException e) {
        throw new FriendlyException("Stream broke when playing OGG track.", SUSPICIOUS, e);
      }
    }, null, persistent == null);
  }

  private void processPersistent(
      OggPacketInputStream packetInputStream,
      AudioProcessingContext context
  ) throws IOException, InterruptedException {
    if (persistent.blueprint == null || persistent.position.actualPosition == 0) {
      persistent.blueprint = OggTrackLoader.loadTrackHandler(packetInputStream);
    } else {
      packetInputStream.startNewTrack();
    }

    OggTrackBlueprint blueprint = persistent.blueprint;

    if (blueprint != null) {
      processSingleTrack(packetInputStream, context, blueprint, persistent.position);
      blueprint.loadTrackHandler(packetInputStream);
    }
  }

  private void processTrackLoop(OggPacketInputStream packetInputStream, AudioProcessingContext context) throws IOException, InterruptedException {
    OggTrackBlueprint blueprint = OggTrackLoader.loadTrackHandler(packetInputStream);

    if (blueprint == null) {
      throw new IOException("Stream terminated before the first packet.");
    }

    while (blueprint != null) {
      processSingleTrack(packetInputStream, context, blueprint, OggTrackPosition.ZERO);
      blueprint = OggTrackLoader.loadTrackHandler(packetInputStream);
    }
  }

  private void processSingleTrack(
      OggPacketInputStream packetInputStream,
      AudioProcessingContext context,
      OggTrackBlueprint blueprint,
      OggTrackPosition position
  ) throws IOException, InterruptedException {
    OggTrackHandler handler = blueprint.loadTrackHandler(packetInputStream);

    try {
      handler.initialise(context, position);
      handler.provideFrames();
    } finally {
      handler.close();
    }
  }

  public static class Persistent {
    public OggTrackPosition position = OggTrackPosition.ZERO;
    public OggTrackBlueprint blueprint;
  }
}
