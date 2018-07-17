package com.sedmelluq.discord.lavaplayer.node;

import com.sedmelluq.discord.lavaplayer.node.message.MessageHandler;
import com.sedmelluq.discord.lavaplayer.node.message.MessageOutput;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerOptions;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.remote.message.TrackExceptionMessage;
import com.sedmelluq.discord.lavaplayer.remote.message.TrackStartRequestMessage;
import com.sedmelluq.discord.lavaplayer.remote.message.TrackStartResponseMessage;
import com.sedmelluq.discord.lavaplayer.remote.message.TrackFrameDataMessage;
import com.sedmelluq.discord.lavaplayer.remote.message.TrackFrameRequestMessage;
import com.sedmelluq.discord.lavaplayer.remote.message.TrackStoppedMessage;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackStateListener;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class PlayingTrackManager {
  private static final long ABANDONED_TRACK_THRESHOLD = TimeUnit.SECONDS.toMillis(10);
  private static final long PAUSED_TRACK_TERMINATE_THRESHOLD = TimeUnit.MINUTES.toMillis(30);
  private static final long PAUSED_TRACK_THRESHOLD = TimeUnit.SECONDS.toMillis(2);

  private static final Logger log = LoggerFactory.getLogger(PlayingTrackManager.class);

  private final StatisticsManager statisticsManager;
  private final DefaultAudioPlayerManager manager;
  private final ConcurrentMap<Long, PlayingTrack> tracks;

  @Autowired
  public PlayingTrackManager(StatisticsManager statisticsManager) {
    this.statisticsManager = statisticsManager;
    manager = new DefaultAudioPlayerManager();
    tracks = new ConcurrentHashMap<>();

    manager.setUseSeekGhosting(false);
    AudioSourceManagers.registerRemoteSources(manager);
  }

  @MessageHandler
  private void handleTrackStart(TrackStartRequestMessage message, MessageOutput output) {
    InternalAudioTrack audioTrack = (InternalAudioTrack) manager.decodeTrackDetails(message.trackInfo, message.encodedTrack);
    String failureReason = null;

    if (audioTrack != null) {
      if (message.position != 0) {
        audioTrack.setPosition(message.position);
      }

      PlayingTrack playingTrack = new PlayingTrack(message.executorId, message.volume, audioTrack);
      PlayingTrack existingTrack = tracks.putIfAbsent(message.executorId, playingTrack);

      if (existingTrack == null) {
        log.info("Track start request for {} (context {}, position {})", message.trackInfo.identifier, message.executorId, message.position);

        manager.executeTrack(playingTrack, audioTrack, message.configuration, playingTrack.playerOptions);
        statisticsManager.increaseTrackCount();
      } else {
        log.info("Start request for an already playing track {} (context {}), applying seek to {} from it.",
            message.trackInfo.identifier, message.executorId, message.position);

        existingTrack.audioTrack.setPosition(message.position);
      }
    } else {
      log.warn("Unable to find a matching source for track {} (context {})", message.trackInfo.identifier, message.executorId);
      failureReason = "This node does not support this type of track.";
    }

    output.send(new TrackStartResponseMessage(message.executorId, failureReason == null, failureReason));
  }

  @MessageHandler
  private void handleTrackDataRequest(TrackFrameRequestMessage message, MessageOutput output) {
    List<AudioFrame> frames = new ArrayList<>();
    PlayingTrack track = tracks.get(message.executorId);
    boolean finished = false;

    if (track != null) {
      submitPendingMessages(track, output);

      track.lastFrameRequestTime = System.currentTimeMillis();
      track.playerOptions.volumeLevel.set(message.volume);

      if (message.seekPosition >= 0) {
        track.audioTrack.setPosition(message.seekPosition);
      }

      if (message.maximumFrames > 0) {
        track.lastNonZeroFrameRequestTime = track.lastFrameRequestTime;
      }

      finished = consumeFramesFromTrack(frames, track.audioTrack, message.maximumFrames);

      if (finished) {
        log.info("Clearing ended track {} (context {})", track.audioTrack.getIdentifier(), message.executorId);
        tracks.remove(message.executorId);
      }
    }

    output.send(new TrackFrameDataMessage(message.executorId, frames, finished, message.seekPosition));
  }

  private void submitPendingMessages(PlayingTrack track, MessageOutput output) {
    TrackExceptionMessage exceptionMessage = track.popExceptionMessage();

    if (exceptionMessage != null) {
      output.send(exceptionMessage);
    }
  }

  private boolean consumeFramesFromTrack(List<AudioFrame> frames, InternalAudioTrack audioTrack, int maximumFrames) {
    AudioFrame frame;

    while (frames.size() < maximumFrames && (frame = audioTrack.provide()) != null) {
      if (frame.isTerminator()) {
        return true;
      } else {
        frames.add(frame);
      }
    }

    return false;
  }

  @MessageHandler
  private void handleTrackStopped(TrackStoppedMessage message) {
    stopTrack(message.executorId, "stop notification");
  }

  private void stopTrack(long executorId, String reason) {
    PlayingTrack track = tracks.remove(executorId);

    if (track != null) {
      log.info("Track {} (context {}) stopped due to {}.", track.audioTrack.getIdentifier(), executorId, reason);

      track.audioTrack.stop();
    }
  }

  @Scheduled(fixedDelay = 5000)
  private void stopAbandonedTracks() {
    long now = System.currentTimeMillis();
    long minimumRequestTime = now - ABANDONED_TRACK_THRESHOLD;
    long minimumNonZeroRequestTime = now - PAUSED_TRACK_TERMINATE_THRESHOLD;
    long minimumPlayingTrackTime = now - PAUSED_TRACK_THRESHOLD;

    int pausedTrackCount = 0;
    int playingTrackCount = 0;

    for (PlayingTrack track : new ArrayList<>(tracks.values())) {
      if (track.lastFrameRequestTime < minimumRequestTime) {
        stopTrack(track.executorId, "no requests for the track");
      } else if (track.lastNonZeroFrameRequestTime < minimumNonZeroRequestTime) {
        stopTrack(track.executorId, "track being stopped for too long");
      } else if (track.lastNonZeroFrameRequestTime < minimumPlayingTrackTime) {
        pausedTrackCount++;
      } else {
        playingTrackCount++;
      }
    }

    statisticsManager.updateTrackStatistics(playingTrackCount, playingTrackCount + pausedTrackCount);
  }

  private static class PlayingTrack implements TrackStateListener {
    private final long executorId;
    private final AudioPlayerOptions playerOptions;
    private final InternalAudioTrack audioTrack;
    private volatile long lastFrameRequestTime;
    private volatile long lastNonZeroFrameRequestTime;
    private AtomicReference<TrackExceptionMessage> exceptionMessage;

    private PlayingTrack(long executorId, int volume, InternalAudioTrack audioTrack) {
      this.executorId = executorId;
      this.playerOptions = new AudioPlayerOptions();
      this.audioTrack = audioTrack;
      this.lastFrameRequestTime = System.currentTimeMillis();
      this.lastNonZeroFrameRequestTime = lastFrameRequestTime;
      this.exceptionMessage = new AtomicReference<>();
      playerOptions.volumeLevel.set(volume);
    }

    @Override
    public void onTrackException(AudioTrack track, FriendlyException exception) {
      this.exceptionMessage.set(new TrackExceptionMessage(executorId, exception));
    }

    @Override
    public void onTrackStuck(AudioTrack track, long thresholdMs) {
      // Should never be called.
    }

    private TrackExceptionMessage popExceptionMessage() {
      return exceptionMessage.getAndSet(null);
    }

    @Override
    public String toString() {
      return "PlayingTrack[executor: " + executorId + "]";
    }
  }
}
