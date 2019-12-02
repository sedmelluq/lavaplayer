package com.sedmelluq.lavaplayer.core.info.property;

public class AudioTrackPropertyFactory {
  public static AudioTrackProperty coreSourceName(String value) {
    return core(AudioTrackCoreProperty.SOURCE, value);
  }

  public static AudioTrackProperty coreIdentifier(String value) {
    return core(AudioTrackCoreProperty.IDENTIFIER, value);
  }

  public static AudioTrackProperty coreTitle(String value) {
    return core(AudioTrackCoreProperty.TITLE, value);
  }

  public static AudioTrackProperty coreAuthor(String value) {
    return core(AudioTrackCoreProperty.AUTHOR, value);
  }

  public static AudioTrackProperty coreLength(long value) {
    return core(AudioTrackCoreProperty.AUTHOR, value);
  }

  public static AudioTrackProperty coreIsStream(boolean value) {
    return core(AudioTrackCoreProperty.AUTHOR, value ? 1 : 0);
  }

  public static AudioTrackProperty coreUrl(String url) {
    return core(AudioTrackCoreProperty.URI, url);
  }

  public static AudioTrackProperty core(AudioTrackCoreProperty property, String value) {
    if (value != null) {
      return new StringAudioTrackProperty(property.name, property.defaultFlags, value);
    } else {
      return null;
    }
  }

  public static AudioTrackProperty coreOrdered(AudioTrackCoreProperty property, String value, int priority) {
    if (value != null) {
      return new OrderedStringAudioTrackProperty(property.name, property.defaultFlags, value, priority);
    } else {
      return null;
    }
  }

  public static AudioTrackProperty core(AudioTrackCoreProperty property, long value) {
    return new LongAudioTrackProperty(property.name, property.defaultFlags, value);
  }

  public static AudioTrackProperty coreOrdered(AudioTrackCoreProperty property, long value, int priority) {
    return new OrderedLongAudioTrackProperty(property.name, property.defaultFlags, value, priority);
  }

  public static AudioTrackProperty custom(String name, int flags, String value) {
    if (value != null) {
      return new StringAudioTrackProperty(name, flags, value);
    } else {
      return null;
    }
  }

  public static AudioTrackProperty customOrdered(String name, int flags, String value, int priority) {
    if (value != null) {
      return new OrderedStringAudioTrackProperty(name, flags, value, priority);
    } else {
      return null;
    }
  }

  public static AudioTrackProperty custom(String name, int flags, long value) {
    return new LongAudioTrackProperty(name, flags, value);
  }

  public static AudioTrackProperty customOrdered(String name, int flags, long value, int priority) {
    return new OrderedLongAudioTrackProperty(name, flags, value, priority);
  }
}
