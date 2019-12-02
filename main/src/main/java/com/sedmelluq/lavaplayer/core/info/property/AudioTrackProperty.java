package com.sedmelluq.lavaplayer.core.info.property;

public abstract class AudioTrackProperty {
  private final String name;
  private final int flags;

  public AudioTrackProperty(String name, int flags) {
    this.name = name;
    this.flags = flags;
  }

  public String getName() {
    return name;
  }

  public int getFlags() {
    return flags;
  }

  public abstract long longValue();

  public abstract String stringValue();

  public int getPriority() {
    return 0;
  }

  public enum Flag {
    METADATA_CORE(1),
    METADATA_EXTENDED(2),
    PLAYBACK_CORE(4),
    PLAYBACK_CACHE(8);

    public int mask;

    Flag(int mask) {
      this.mask = mask;
    }

    public static int fullMask() {
      return METADATA_CORE.mask | METADATA_EXTENDED.mask | PLAYBACK_CORE.mask | PLAYBACK_CACHE.mask;
    }
  }
}
