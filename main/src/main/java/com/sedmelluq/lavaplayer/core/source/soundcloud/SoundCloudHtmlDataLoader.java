package com.sedmelluq.lavaplayer.core.source.soundcloud;

import com.sedmelluq.lavaplayer.core.tools.JsonBrowser;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import java.io.IOException;

public interface SoundCloudHtmlDataLoader {
  JsonBrowser load(HttpInterface httpInterface, String url) throws IOException;
}
