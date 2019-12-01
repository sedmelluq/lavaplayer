package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import java.io.IOException;

public interface SoundCloudHtmlDataLoader {
  JsonBrowser load(HttpInterface httpInterface, String url) throws IOException;
}
