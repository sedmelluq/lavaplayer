package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.container.ogg.OggAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.stream.ExtendedM3uParser;
import com.sedmelluq.discord.lavaplayer.source.stream.M3uStreamAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.stream.M3uStreamSegmentUrlProvider;
import com.sedmelluq.discord.lavaplayer.tools.io.ChainedInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.NonSeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

public class SoundCloudOpusM3uAudioTrack extends M3uStreamAudioTrack {
  private final HttpInterface httpInterface;
  private final SegmentUrlProvider segmentUrlProvider;
  private final String streamBaseUrl;

  public SoundCloudOpusM3uAudioTrack(AudioTrackInfo trackInfo, HttpInterface httpInterface, String streamBaseUrl) {
    super(trackInfo);
    this.httpInterface = httpInterface;
    this.streamBaseUrl = streamBaseUrl;
    this.segmentUrlProvider = new SegmentUrlProvider();
  }

  @Override
  public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
    segmentUrlProvider.initializeUrls(httpInterface);

    try (
        HttpInterface httpInterface = getHttpInterface();
        ChainedInputStream chainedInputStream = new ChainedInputStream(() ->
            getSegmentUrlProvider() .getNextSegmentStream(httpInterface)
        )
    ) {
      processJoinedStream(localExecutor, chainedInputStream);
    }
  }

  @Override
  protected M3uStreamSegmentUrlProvider getSegmentUrlProvider() {
    return segmentUrlProvider;
  }

  @Override
  protected HttpInterface getHttpInterface() {
    return httpInterface;
  }

  @Override
  protected void processJoinedStream(LocalAudioTrackExecutor localExecutor, InputStream stream) throws Exception {
    processDelegate(new OggAudioTrack(trackInfo, new NonSeekableInputStream(stream)), localExecutor);
  }

  private class SegmentUrlProvider extends M3uStreamSegmentUrlProvider {
    private final List<String> urls = new ArrayList<>();
    private int position = 0;

    @Override
    protected String getQualityFromM3uDirective(ExtendedM3uParser.Line directiveLine) {
      return "normal";
    }

    @Override
    protected String fetchSegmentPlaylistUrl(HttpInterface httpInterface) {
      return streamBaseUrl;
    }

    @Override
    protected HttpUriRequest createSegmentGetRequest(String url) {
      return new HttpGet(url);
    }

    @Override
    protected String getNextSegmentUrl(HttpInterface httpInterface) {
      int current = position++;

      if (current < urls.size()) {
        return urls.get(current);
      } else {
        return null;
      }
    }

    public void initializeUrls(HttpInterface httpInterface) throws IOException {
      for (SegmentInfo info : loadStreamSegmentsList(httpInterface, fetchSegmentPlaylistUrl(httpInterface))) {
        urls.add(info.url);
      }
    }
  }
}
