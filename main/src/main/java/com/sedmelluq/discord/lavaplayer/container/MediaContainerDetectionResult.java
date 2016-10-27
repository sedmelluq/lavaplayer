package com.sedmelluq.discord.lavaplayer.container;

import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

/**
 * Result of audio container detection.
 */
public class MediaContainerDetectionResult {
  private final AudioTrackInfo trackInfo;
  private final MediaContainerProbe containerProbe;
  private final AudioReference reference;
  private final String unsupportedReason;

  /**
   * Constructor for supported file.
   *
   * @param containerProbe Probe of the container
   * @param trackInfo Track info for the file
   */
  public MediaContainerDetectionResult(MediaContainerProbe containerProbe, AudioTrackInfo trackInfo) {
    this.trackInfo = trackInfo;
    this.containerProbe = containerProbe;
    this.unsupportedReason = null;
    this.reference = null;
  }

  /**
   * Constructor for load result referring to another item.
   *
   * @param containerProbe Probe of the container
   * @param reference Reference to another item
   */
  public MediaContainerDetectionResult(MediaContainerProbe containerProbe, AudioReference reference) {
    this.trackInfo = null;
    this.containerProbe = containerProbe;
    this.unsupportedReason = null;
    this.reference = reference;
  }

  /**
   * Constructor for unsupported file of a known container.
   *
   * @param containerProbe Probe of the container
   * @param unsupportedReason The reason why this track is not supported
   */
  public MediaContainerDetectionResult(MediaContainerProbe containerProbe, String unsupportedReason) {
    this.trackInfo = null;
    this.containerProbe = containerProbe;
    this.unsupportedReason = unsupportedReason;
    this.reference = null;
  }

  /**
   * Constructor for unknown format result.
   */
  public MediaContainerDetectionResult() {
    trackInfo = null;
    containerProbe = null;
    unsupportedReason = null;
    reference = null;
  }

  /**
   * @return If the container this file uses was detected. In case this returns true, the container probe is non-null.
   */
  public boolean isContainerDetected() {
    return containerProbe != null;
  }

  /**
   * @return The probe for the container of the file
   */
  public MediaContainerProbe getContainerProbe() {
    return containerProbe;
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
  public AudioTrackInfo getTrackInfo() {
    return trackInfo;
  }

  public boolean isReference() {
    return reference != null;
  }

  public AudioReference getReference() {
    return reference;
  }
}
