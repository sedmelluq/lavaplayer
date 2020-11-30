package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.source.youtube.format.LegacyAdaptiveFormatsExtractor;
import com.sedmelluq.discord.lavaplayer.source.youtube.format.LegacyDashMpdFormatsExtractor;
import com.sedmelluq.discord.lavaplayer.source.youtube.format.LegacyStreamMapFormatsExtractor;
import com.sedmelluq.discord.lavaplayer.source.youtube.format.StreamingDataFormatsExtractor;
import com.sedmelluq.discord.lavaplayer.source.youtube.format.YoutubeTrackFormatExtractor;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.Units;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;
import static com.sedmelluq.discord.lavaplayer.tools.Units.DURATION_MS_UNKNOWN;

public class DefaultYoutubeTrackDetails implements YoutubeTrackDetails {
  private static final Logger log = LoggerFactory.getLogger(DefaultYoutubeTrackDetails.class);

  private static final YoutubeTrackFormatExtractor[] FORMAT_EXTRACTORS = new YoutubeTrackFormatExtractor[] {
      new LegacyAdaptiveFormatsExtractor(),
      new StreamingDataFormatsExtractor(),
      new LegacyDashMpdFormatsExtractor(),
      new LegacyStreamMapFormatsExtractor()
  };

  private final String videoId;
  private final YoutubeTrackJsonData data;

  public DefaultYoutubeTrackDetails(String videoId, YoutubeTrackJsonData data) {
    this.videoId = videoId;
    this.data = data;
  }

  @Override
  public AudioTrackInfo getTrackInfo() {
    return loadTrackInfo();
  }

  @Override
  public List<YoutubeTrackFormat> getFormats(HttpInterface httpInterface, YoutubeSignatureResolver signatureResolver) {
    try {
      return loadTrackFormats(httpInterface, signatureResolver);
    } catch (Exception e) {
      throw ExceptionTools.toRuntimeException(e);
    }
  }

  @Override
  public String getPlayerScript() {
    return data.playerScriptUrl;
  }

  private List<YoutubeTrackFormat> loadTrackFormats(
      HttpInterface httpInterface,
      YoutubeSignatureResolver signatureResolver
  ) {
    for (YoutubeTrackFormatExtractor extractor : FORMAT_EXTRACTORS) {
      List<YoutubeTrackFormat> formats = extractor.extract(data, httpInterface, signatureResolver);

      if (!formats.isEmpty()) {
        return formats;
      }
    }

    log.warn(
        "Video {} with no detected format field, response {} polymer {}",
        videoId,
        data.playerResponse.format(),
        data.polymerArguments.format()
    );

    throw new FriendlyException("Unable to play this YouTube track.", SUSPICIOUS,
        new IllegalStateException("No track formats found."));
  }

  private AudioTrackInfo loadTrackInfo() {
    JsonBrowser playabilityStatus = data.playerResponse.get("playabilityStatus");

    if ("ERROR".equals(playabilityStatus.get("status").text())) {
      throw new FriendlyException(playabilityStatus.get("reason").text(), COMMON, null);
    }

    JsonBrowser videoDetails = data.playerResponse.get("videoDetails");

    if (videoDetails.isNull()) {
      return loadLegacyTrackInfo();
    }

    TemporalInfo temporalInfo = TemporalInfo.fromRawData(
        videoDetails.get("isLiveContent").asBoolean(false),
        videoDetails.get("lengthSeconds")
    );

    return buildTrackInfo(videoId, videoDetails.get("title").text(), videoDetails.get("author").text(), temporalInfo);
  }

  private AudioTrackInfo loadLegacyTrackInfo() {
    JsonBrowser args = data.polymerArguments;

    if ("fail".equals(args.get("status").text())) {
      throw new FriendlyException(args.get("reason").text(), COMMON, null);
    }

    TemporalInfo temporalInfo = TemporalInfo.fromRawData(
        "1".equals(args.get("live_playback").text()),
        args.get("length_seconds")
    );

    return buildTrackInfo(videoId, args.get("title").text(), args.get("author").text(), temporalInfo);
  }

  private AudioTrackInfo buildTrackInfo(String videoId, String title, String uploader, TemporalInfo temporalInfo) {
    return new AudioTrackInfo(title, uploader, temporalInfo.durationMillis, videoId, temporalInfo.isActiveStream,
        "https://www.youtube.com/watch?v=" + videoId);
  }

  private static class TemporalInfo {
    public final boolean isActiveStream;
    public final long durationMillis;

    private TemporalInfo(boolean isActiveStream, long durationMillis) {
      this.isActiveStream = isActiveStream;
      this.durationMillis = durationMillis;
    }

    public static TemporalInfo fromRawData(boolean wasLiveStream, JsonBrowser durationSecondsField) {
      long durationValue = durationSecondsField.asLong(0L);
      // VODs are not really live streams, even though that field in JSON claims they are. If it is actually live, then
      // duration is also missing or 0.
      boolean isActiveStream = wasLiveStream && durationValue == 0;

      return new TemporalInfo(
          isActiveStream,
          durationValue == 0 ? DURATION_MS_UNKNOWN : Units.secondsToMillis(durationValue)
      );
    }
  }
}
