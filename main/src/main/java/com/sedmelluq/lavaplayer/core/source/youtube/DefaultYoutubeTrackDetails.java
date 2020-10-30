package com.sedmelluq.lavaplayer.core.source.youtube;

import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfo;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoTemplate;
import com.sedmelluq.lavaplayer.core.source.youtube.format.LegacyAdaptiveFormatsExtractor;
import com.sedmelluq.lavaplayer.core.source.youtube.format.LegacyDashMpdFormatsExtractor;
import com.sedmelluq.lavaplayer.core.source.youtube.format.LegacyStreamMapFormatsExtractor;
import com.sedmelluq.lavaplayer.core.source.youtube.format.StreamingDataFormatsExtractor;
import com.sedmelluq.lavaplayer.core.source.youtube.format.YoutubeTrackFormatExtractor;
import com.sedmelluq.lavaplayer.core.tools.JsonBrowser;
import com.sedmelluq.lavaplayer.core.tools.Units;
import com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.tools.Units.DURATION_MS_UNKNOWN;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.COMMON;
import static com.sedmelluq.lavaplayer.core.tools.exception.FriendlyException.Severity.SUSPICIOUS;

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
  private final AudioTrackInfoTemplate template;

  public DefaultYoutubeTrackDetails(String videoId, YoutubeTrackJsonData data, AudioTrackInfoTemplate template) {
    this.videoId = videoId;
    this.data = data;
    this.template = template;
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
      throw new RuntimeException(e);
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

    return buildTrackInfo(videoId, videoDetails, temporalInfo);
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

    return buildTrackInfo(videoId, args, temporalInfo);
  }

  private AudioTrackInfo buildTrackInfo(String videoId, JsonBrowser holder, TemporalInfo temporalInfo) {
    return YoutubeTrackInfoFactory.create(
        template,
        videoId,
        holder.get("author").text(),
        holder.get("title").text(),
        temporalInfo.durationMillis,
        temporalInfo.isActiveStream
    );
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
