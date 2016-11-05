package com.sedmelluq.discord.lavaplayer.container.mpeg;

/**
 * Codec information for an MP4 track
 */
public class MpegTrackInfo {
  /**
   * ID of the track
   */
  public final int trackId;
  /**
   * Handler type (soun for audio)
   */
  public final String handler;
  /**
   * Name of the codec
   */
  public final String codecName;
  /**
   * Number of audio channels
   */
  public final int channelCount;
  /**
   * Sample rate for audio
   */
  public final int sampleRate;

  /**
   * @param trackId ID of the track
   * @param handler Handler type (soun for audio)
   * @param codecName Name of the codec
   * @param channelCount Number of audio channels
   * @param sampleRate Sample rate for audio
   */
  public MpegTrackInfo(int trackId, String handler, String codecName, int channelCount, int sampleRate) {
    this.trackId = trackId;
    this.handler = handler;
    this.codecName = codecName;
    this.channelCount = channelCount;
    this.sampleRate = sampleRate;
  }

  /**
   * Helper class for constructing a track info instance.
   */
  public static class Builder {
    private int trackId;
    private String handler;
    private String codecName;
    private int channelCount;
    private int sampleRate;

    public void setTrackId(int trackId) {
      this.trackId = trackId;
    }

    public int getTrackId() {
      return trackId;
    }

    public String getHandler() {
      return handler;
    }

    public void setHandler(String handler) {
      this.handler = handler;
    }

    public void setCodecName(String codecName) {
      this.codecName = codecName;
    }

    public void setChannelCount(int channelCount) {
      this.channelCount = channelCount;
    }

    public void setSampleRate(int sampleRate) {
      this.sampleRate = sampleRate;
    }

    /**
     * @return The final track info
     */
    public MpegTrackInfo build() {
      return new MpegTrackInfo(trackId, handler, codecName, channelCount, sampleRate);
    }
  }
}
