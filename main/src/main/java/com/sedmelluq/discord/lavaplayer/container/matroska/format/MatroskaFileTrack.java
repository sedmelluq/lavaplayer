package com.sedmelluq.discord.lavaplayer.container.matroska.format;

import java.io.IOException;

/**
 * Describes one track in a matroska file.
 */
public class MatroskaFileTrack {
  /**
   * Track index/number.
   */
  public final int index;
  /**
   * Type of the track.
   */
  public final Type type;
  /**
   * The unique track UID.
   */
  public final long trackUid;
  /**
   * Name of the track.
   */
  public final String name;
  /**
   * ID of the codec.
   */
  public final String codecId;
  /**
   * Custom data for the codec (header).
   */
  public final byte[] codecPrivate;
  /**
   * Information specific to audio tracks (null for non-audio tracks).
   */
  public final AudioDetails audio;

  /**
   * @param index Track index/number.
   * @param type Type of the track.
   * @param trackUid The unique track UID.
   * @param name Name of the track.
   * @param codecId ID of the codec.
   * @param codecPrivate Custom data for the codec (header).
   * @param audio Information specific to audio tracks (null for non-audio tracks).
   */
  public MatroskaFileTrack(int index, Type type, long trackUid, String name, String codecId, byte[] codecPrivate, AudioDetails audio) {
    this.index = index;
    this.type = type;
    this.trackUid = trackUid;
    this.name = name;
    this.codecId = codecId;
    this.codecPrivate = codecPrivate;
    this.audio = audio;
  }

  /**
   * Track type list.
   */
  public enum Type {
    VIDEO(1),
    AUDIO(2),
    COMPLEX(3),
    LOGO(0x10),
    SUBTITLE(0x11),
    BUTTONS(0x12),
    CONTROL(0x20);

    /**
     * ID which is used in the track type field in the file.
     */
    public final long id;

    Type(long id) {
      this.id = id;
    }

    /**
     * @param id ID to look up.
     * @return Track type for that ID, null if not found.
     */
    public static Type fromId(long id) {
      for (Type entry : Type.class.getEnumConstants()) {
        if (entry.id == id) {
          return entry;
        }
      }

      return null;
    }
  }

  /**
   * Fields specific to an audio track.
   */
  public static class AudioDetails {
    /**
     * Sampling frequency in Hz.
     */
    public final float samplingFrequency;
    /**
     * Real output sampling frequency in Hz.
     */
    public final float outputSamplingFrequency;
    /**
     * Number of channels in the track.
     */
    public final int channels;
    /**
     * Number of bits per sample.
     */
    public final int bitDepth;

    /**
     * @param samplingFrequency Sampling frequency in Hz.
     * @param outputSamplingFrequency Real output sampling frequency in Hz.
     * @param channels Number of channels in the track.
     * @param bitDepth Number of bits per sample.
     */
    public AudioDetails(float samplingFrequency, float outputSamplingFrequency, int channels, int bitDepth) {
      this.samplingFrequency = samplingFrequency;
      this.outputSamplingFrequency = outputSamplingFrequency;
      this.channels = channels;
      this.bitDepth = bitDepth;
    }
  }

  /**
   * @param trackElement The track element
   * @param reader Matroska file reader
   * @return The parsed track
   * @throws IOException On read error
   */
  public static MatroskaFileTrack parse(MatroskaElement trackElement, MatroskaFileReader reader) throws IOException {
    Builder builder = new Builder();
    MatroskaElement child;

    while ((child = reader.readNextElement(trackElement)) != null) {
      if (child.is(MatroskaElementType.TrackNumber)) {
        builder.index = reader.asInteger(child);
      } else if (child.is(MatroskaElementType.TrackUid)) {
        builder.trackUid = reader.asLong(child);
      } else if (child.is(MatroskaElementType.TrackType)) {
        builder.type = Type.fromId(reader.asInteger(child));
      } else if (child.is(MatroskaElementType.Name)) {
        builder.name = reader.asString(child);
      } else if (child.is(MatroskaElementType.CodecId)) {
        builder.codecId = reader.asString(child);
      } else if (child.is(MatroskaElementType.CodecPrivate)) {
        builder.codecPrivate = reader.asBytes(child);
      } else if (child.is(MatroskaElementType.Audio)) {
        builder.audio = parseAudioElement(child, reader);
      }

      // Unused fields: DefaultDuration, Language, Video, etc
      reader.skip(child);
    }

    return builder.build();
  }

  private static AudioDetails parseAudioElement(MatroskaElement audioElement, MatroskaFileReader reader) throws IOException {
    AudioBuilder builder = new AudioBuilder();
    MatroskaElement child;

    while ((child = reader.readNextElement(audioElement)) != null) {
      if (child.is(MatroskaElementType.SamplingFrequency)) {
        builder.samplingFrequency = reader.asFloat(child);
      } else if (child.is(MatroskaElementType.OutputSamplingFrequency)) {
        builder.outputSamplingFrequency = reader.asFloat(child);
      } else if (child.is(MatroskaElementType.Channels)) {
        builder.channels = reader.asInteger(child);
      } else if (child.is(MatroskaElementType.BitDepth)) {
        builder.bitDepth = reader.asInteger(child);
      }

      reader.skip(child);
    }

    return builder.build();
  }

  private static class Builder {
    private int index;
    private Type type;
    private long trackUid;
    private String name;
    private String codecId;
    private byte[] codecPrivate;
    private AudioDetails audio;

    private MatroskaFileTrack build() {
      return new MatroskaFileTrack(index, type, trackUid, name, codecId, codecPrivate, audio);
    }
  }

  private static class AudioBuilder {
    private float samplingFrequency;
    private float outputSamplingFrequency;
    private int channels;
    private int bitDepth;

    private AudioDetails build() {
      return new AudioDetails(samplingFrequency, outputSamplingFrequency, channels, bitDepth);
    }
  }
}
