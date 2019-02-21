package com.sedmelluq.discord.lavaplayer.integration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalFormatSampleIndex {
  private static final Pattern sampleRatePattern = Pattern.compile("demo-[a-z0-9]+-([0-9]+)");

  // Decode results with these CRC values have been confirmed to sound fine
  private static final long CRC_LOSSLESS_44100 = 3154656839L;
  private static final long CRC_LOSSLESS_48000 = 3338957453L;

  public static final Sample[] SAMPLES = new Sample[] {
      new Sample("demo-adts-48000.aac", 2364196011L, 3901196231L),
      new Sample("demo-flac-44100-16bit.flac", CRC_LOSSLESS_44100),
      new Sample("demo-flac-48000-8bit.flac", 795405597L),
      new Sample("demo-flac-48000-16bit.flac", CRC_LOSSLESS_48000),
      new Sample("demo-flac-48000-24bit.flac", CRC_LOSSLESS_48000),
      new Sample("demo-mp3cbr-44100.mp3", 487964262L),
      new Sample("demo-mp3cbr-48000.mp3", 2752771400L),
      new Sample("demo-mp3vbr-48000.mp3", 2122647494L),
      new Sample("demo-oggflac-44100-16bit.ogg", CRC_LOSSLESS_44100),
      new Sample("demo-oggflac-48000-16bit.ogg", CRC_LOSSLESS_48000),
      new Sample("demo-oggopus-48000.ogg", 3781942969L, 3487298184L),
      new Sample("demo-oggvorbis-44100.ogg", 3562794104L, 3488695464L),
      new Sample("demo-oggvorbis-48000.ogg", 863664551L, 118615525L),
      new Sample("demo-tsadts-48000.ts", 2364196011L, 3901196231L),
      new Sample("demo-wav-44100-16bit.wav", CRC_LOSSLESS_44100),
      new Sample("demo-wav-48000-16bit.wav", CRC_LOSSLESS_48000)
  };

  public static class Sample {
    public final String filename;
    public final long[] validCrcs;

    public Sample(String filename, long... validCrcs) {
      this.filename = filename;
      this.validCrcs = validCrcs;
    }

    public int getSampleRate() {
      Matcher matcher = sampleRatePattern.matcher(filename);
      if (!matcher.find()) {
        throw new RuntimeException("Sample file name has invalid format.");
      }

      return Integer.valueOf(matcher.group(1));
    }

    @Override
    public String toString() {
      return filename;
    }
  }
}
