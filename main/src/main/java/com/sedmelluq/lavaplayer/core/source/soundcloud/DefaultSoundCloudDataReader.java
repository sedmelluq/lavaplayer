package com.sedmelluq.lavaplayer.core.source.soundcloud;

import com.sedmelluq.lavaplayer.core.info.track.AudioTrackInfoBuilder;
import com.sedmelluq.lavaplayer.core.tools.JsonBrowser;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreAuthor;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIdentifier;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreIsStream;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreLength;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreTitle;
import static com.sedmelluq.lavaplayer.core.info.property.AudioTrackPropertyFactory.coreUrl;

public class DefaultSoundCloudDataReader implements SoundCloudDataReader {
  private static final Logger log = LoggerFactory.getLogger(DefaultSoundCloudDataReader.class);

  @Override
  public JsonBrowser findTrackData(JsonBrowser rootData) {
    return findEntryOfKind(rootData, "track");
  }

  @Override
  public String readTrackId(JsonBrowser trackData) {
    return trackData.get("id").safeText();
  }

  @Override
  public boolean isTrackBlocked(JsonBrowser trackData) {
    return "BLOCK".equals(trackData.get("policy").safeText());
  }

  @Override
  public void readTrackInfo(JsonBrowser trackData, String identifier, AudioTrackInfoBuilder builder) {
    builder
        .with(coreTitle(trackData.get("title").safeText()))
        .with(coreAuthor(trackData.get("user").get("username").safeText()))
        .with(coreLength(trackData.get("duration").as(Integer.class)))
        .with(coreIdentifier(identifier))
        .with(coreIsStream(false))
        .with(coreUrl(trackData.get("permalink_url").text()));
  }

  @Override
  public List<SoundCloudTrackFormat> readTrackFormats(JsonBrowser trackData) {
    List<SoundCloudTrackFormat> formats = new ArrayList<>();
    String trackId = readTrackId(trackData);

    if (trackId.isEmpty()) {
      log.warn("Track data {} missing track ID: {}.", trackId, trackData.format());
    }

    for (JsonBrowser transcoding : trackData.get("media").get("transcodings").values()) {
      JsonBrowser format = transcoding.get("format");

      String protocol = format.get("protocol").safeText();
      String mimeType = format.get("mime_type").safeText();

      if (!protocol.isEmpty() && !mimeType.isEmpty()) {
        String lookupUrl = transcoding.get("url").safeText();

        if (!lookupUrl.isEmpty()) {
          formats.add(new DefaultSoundCloudTrackFormat(trackId, protocol, mimeType, lookupUrl));
        } else {
          log.warn("Transcoding of {} missing url: {}.", trackId, transcoding.format());
        }
      } else {
        log.warn("Transcoding of {} missing protocol/mimetype: {}.", trackId, transcoding.format());
      }
    }

    return formats;
  }

  @Override
  public JsonBrowser findPlaylistData(JsonBrowser rootData) {
    return findEntryOfKind(rootData, "playlist");
  }

  @Override
  public String readPlaylistName(JsonBrowser playlistData) {
    return playlistData.get("title").safeText();
  }

  @Override
  public String readPlaylistIdentifier(JsonBrowser playlistData) {
    return playlistData.get("permalink").safeText();
  }

  @Override
  public List<JsonBrowser> readPlaylistTracks(JsonBrowser playlistData) {
    return playlistData.get("tracks").values();
  }

  protected JsonBrowser findEntryOfKind(JsonBrowser data, String kind) {
    for (JsonBrowser value : data.values()) {
      for (JsonBrowser entry : value.get("data").values()) {
        if (entry.isMap() && kind.equals(entry.get("kind").text())) {
          return entry;
        }
      }
    }

    return null;
  }
}
