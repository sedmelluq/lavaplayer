package com.sedmelluq.discord.lavaplayer.source.soundcloud;

public class SoundCloudHelper {
  public static String nonMobileUrl(String url) {
    if (url.startsWith("https://m.")) {
      return "https://" + url.substring("https://m.".length());
    } else {
      return url;
    }
  }
}
