package com.sedmelluq.lavaplayer.core.container;

import com.sedmelluq.lavaplayer.core.info.request.AudioInfoRequest;
import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoBuilder;

/**
 * Result of audio container detection.
 */
public class MediaContainerDetectionResult {
  private static final MediaContainerDetectionResult UNKNOWN_FORMAT =
      new MediaContainerDetectionResult(null, null, null, null);

  private final AudioTrackInfoBuilder trackInfoBuilder;
  private final MediaContainerProbe containerProbe;
  private final AudioInfoRequest request;
  private final String unsupportedReason;

  private MediaContainerDetectionResult(
      AudioTrackInfoBuilder trackInfoBuilder,
      MediaContainerProbe containerProbe,
      AudioInfoRequest request,
      String unsupportedReason
  ) {
    this.trackInfoBuilder = trackInfoBuilder;
    this.containerProbe = containerProbe;
    this.request = request;
    this.unsupportedReason = unsupportedReason;
  }

  public MediaContainerProbe getContainerProbe() {
    return containerProbe;
  }

  /**
   * Creates an unknown format result.
   */
  public static MediaContainerDetectionResult unknownFormat() {
    return UNKNOWN_FORMAT;
  }

  /**
   * Creates a result ofr an unsupported file of a known container.
   *
   * @param probe Probe of the container
   * @param reason The reason why this track is not supported
   */
  public static MediaContainerDetectionResult unsupportedFormat(MediaContainerProbe probe, String reason) {
    return new MediaContainerDetectionResult(null, probe, null, reason);
  }

  /**
   * Creates a load result referring to another item.
   *
   * @param probe Probe of the container
   */
  public static MediaContainerDetectionResult refer(MediaContainerProbe probe, AudioInfoRequest request) {
    return new MediaContainerDetectionResult(null, probe, request, null);
  }


  /**
   * Creates a load result for supported file.
   *
   * @param probe Probe of the container
   */
  public static MediaContainerDetectionResult supportedFormat(
      MediaContainerProbe probe,
      AudioTrackInfoBuilder trackInfoBuilder
  ) {
    return new MediaContainerDetectionResult(trackInfoBuilder, probe, null, null);
  }

  /**
   * @return If the container this file uses was detected. In case this returns true, the container probe is non-null.
   */
  public boolean isContainerDetected() {
    return containerProbe != null;
  }

  /**
   * @return Whether this specific file is supported. If this returns true, the track info is non-null. Otherwise
   *         the reason why this file is not supported can be retrieved via getUnsupportedReason().
   */
  public boolean isSupportedFile() {
    return isContainerDetected() && unsupportedReason == null;
  }

  /**
   * @return The reason why this track is not supported.
   */
  public String getUnsupportedReason() {
    return unsupportedReason;
  }

  /**
   * @return Track info for the detected file.
   */
  public AudioTrackInfoBuilder getTrackInfoBuilder() {
    return trackInfoBuilder;
  }

  public boolean isRequest() {
    return request != null;
  }

  public AudioInfoRequest getRequest() {
    return request;
  }
}
