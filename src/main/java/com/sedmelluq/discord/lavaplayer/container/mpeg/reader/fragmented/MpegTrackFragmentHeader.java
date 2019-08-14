package com.sedmelluq.discord.lavaplayer.container.mpeg.reader.fragmented;

/**
 * Header for an MP4 track in a fragment.
 */
public class MpegTrackFragmentHeader {
  /**
   * Track ID which this header is for
   */
  public final int trackId;
  /**
   * The timecode at which this track is at the start of this fragment
   */
  public final long baseTimecode;
  /**
   * The offset of the data for this track in this fragment
   */
  public final int dataOffset;
  /**
   * Duration of each sample for this track in this fragment using file timescale
   */
  public final int[] sampleDurations;
  /**
   * Size of each sample for this track in this fragment
   */
  public final int[] sampleSizes;

  /**
   * @param trackId Track ID which this header is for
   * @param baseTimecode The timecode at which this track is at the start of this fragment
   * @param dataOffset The offset of the data for this track in this fragment
   * @param sampleDurations Duration of each sample for this track in this fragment using file timescale
   * @param sampleSizes Size of each sample for this track in this fragment
   */
  public MpegTrackFragmentHeader(int trackId, long baseTimecode, int dataOffset, int[] sampleDurations, int[] sampleSizes) {
    this.trackId = trackId;
    this.baseTimecode = baseTimecode;
    this.dataOffset = dataOffset;
    this.sampleDurations = sampleDurations;
    this.sampleSizes = sampleSizes;
  }

  /**
   * A helper for building an instance of this class.
   */
  public static class Builder {
    private int trackId;
    private long baseTimecode;
    private int dataOffset;
    private int defaultSampleSize;
    private int sampleCount;
    private int[] sampleDurations;
    private int[] sampleSizes;

    /**
     * Create an empty builder.
     */
    public Builder() {
      trackId = -1;
    }

    /**
     * @return Previously assigned track ID, or -1 if not assigned
     */
    public int getTrackId() {
      return trackId;
    }

    public void setTrackId(int trackId) {
      this.trackId = trackId;
    }

    public void setBaseTimecode(long baseTimecode) {
      this.baseTimecode = baseTimecode;
    }

    public void setDataOffset(int dataOffset) {
      this.dataOffset = dataOffset;
    }

    public void setDefaultSampleSize(int defaultSampleSize) {
      this.defaultSampleSize = defaultSampleSize;
    }

    /**
     * Create sample duration and size arrays
     * @param hasDurations If duration data is present
     * @param hasSizes If size data is present
     * @param sampleCount Number of samples
     */
    public void createSampleArrays(boolean hasDurations, boolean hasSizes, int sampleCount) {
      this.sampleCount = sampleCount;

      if (hasDurations) {
        sampleDurations = new int[sampleCount];
      }

      if (hasSizes) {
        sampleSizes = new int[sampleCount];
      }
    }

    /**
     * Set the duration of a specific sample
     * @param i Sample index
     * @param value Duration using the file timescale
     */
    public void setDuration(int i, int value) {
      sampleDurations[i] = value;
    }

    /**
     * Set the size of a specific sample
     * @param i Sample index
     * @param value Size
     */
    public void setSize(int i, int value) {
      sampleSizes[i] = value;
    }

    /**
     * @return The final header
     */
    public MpegTrackFragmentHeader build() {
      int[] finalSampleSizes = sampleSizes;

      if (defaultSampleSize != 0) {
        finalSampleSizes = new int[sampleCount];

        for (int i = 0; i < sampleCount; i++) {
          finalSampleSizes[i] = defaultSampleSize;
        }
      }

      return new MpegTrackFragmentHeader(trackId, baseTimecode, dataOffset, sampleDurations, finalSampleSizes);
    }
  }
}
