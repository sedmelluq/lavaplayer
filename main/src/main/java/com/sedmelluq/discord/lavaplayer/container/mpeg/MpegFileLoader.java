package com.sedmelluq.discord.lavaplayer.container.mpeg;

import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.MpegParseStopChecker;
import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.MpegVersionedSectionInfo;
import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.fragmented.MpegFragmentedFileTrackProvider;
import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.MpegFileTrackProvider;
import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.MpegReader;
import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.MpegSectionInfo;
import com.sedmelluq.discord.lavaplayer.container.mpeg.reader.standard.MpegStandardFileTrackProvider;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles processing an MP4 file for the purpose of streaming one specific track from it. Only performs seeks when
 * absolutely necessary, as the stream may be a network connection, in which case each seek may require a new connection.
 */
public class MpegFileLoader {
  private final List<MpegTrackInfo> tracks;
  private final MpegFragmentedFileTrackProvider fragmentedFileReader;
  private final MpegStandardFileTrackProvider standardFileReader;
  private final MpegReader reader;
  private final MpegSectionInfo root;
  private final Map<String, Object> metadata;
  private byte[] lastEventMessage;

  /**
   * @param inputStream Stream to read the file from
   */
  public MpegFileLoader(SeekableInputStream inputStream) {
    this.tracks = new ArrayList<>();
    this.reader = new MpegReader(inputStream);
    this.root = new MpegSectionInfo(0, inputStream.getContentLength(), "root");
    this.fragmentedFileReader = new MpegFragmentedFileTrackProvider(reader, root);
    this.standardFileReader = new MpegStandardFileTrackProvider(reader);
    this.metadata = new HashMap<>();
  }

  /**
   * @return List of tracks found in the file
   */
  public List<MpegTrackInfo> getTrackList() {
    return tracks;
  }

  /**
   * Read the headers of the file to get the list of tracks and data required for seeking.
   */
  public void parseHeaders() {
    try {
      final AtomicBoolean movieBoxSeen = new AtomicBoolean();

      reader.in(root).handle("moov", moov -> {
        movieBoxSeen.set(true);

        reader.in(moov).handle("trak",
            this::parseTrackInfo
        ).handle("mvex",
            fragmentedFileReader::parseMovieExtended
        ).handle("udta",
            this::parseMetadata
        ).run();
      }).handleVersioned("emsg",
          this::parseEventMessage
      ).handleVersioned("sidx", true,
          fragmentedFileReader::parseSegmentIndex
      ).stopChecker(getRootStopChecker(movieBoxSeen)).run();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param name Name of the text metadata field.
   * @return Value of the metadata field, or null if no value or not a string.
   */
  public String getTextMetadata(String name) {
    Object data = metadata.get(name);
    return data instanceof String ? (String) data : null;
  }

  /**
   * @return Payload from the last emsg message encountered.
   */
  public byte[] getLastEventMessage() {
    return lastEventMessage;
  }

  private void parseMetadata(MpegSectionInfo udta) throws IOException {
    reader.in(udta).handleVersioned("meta", meta -> {
      reader.in(meta).handle("ilst", ilst -> {
        MpegSectionInfo entry;

        while ((entry = reader.nextChild(ilst)) != null) {
          parseMetadataEntry(entry);
        }
      }).run();
    }).run();
  }

  private void parseMetadataEntry(MpegSectionInfo entry) throws IOException {
    MpegSectionInfo dataHeader = reader.nextChild(entry);

    if (dataHeader != null && "data".equals(dataHeader.type)) {
      MpegVersionedSectionInfo data = reader.parseFlags(dataHeader);

      // Skip next 4 bytes
      reader.data.readInt();

      if (data.flags == 1) {
        storeMetadata(entry.type, reader.readUtfString((int) data.length - 16));
      }
    }

    reader.skip(entry);
  }

  private void storeMetadata(String code, Object value) {
    String name = getMetadataName(code);

    if (name != null && value != null) {
      metadata.put(name, value);
    }
  }

  private static String getMetadataName(String code) {
    switch (code.toLowerCase()) {
      case "\u00a9art": return "Artist";
      case "\u00a9nam": return "Title";
      default: return null;
    }
  }

  private MpegParseStopChecker getRootStopChecker(final AtomicBoolean movieBoxSeen) {
    return (child, start) -> {
      if (!start && "sidx".equals(child.type)) {
        return true;
      } else if (start && ("mdat".equals(child.type) || "free".equals(child.type))) {
        return movieBoxSeen.get();
      } else {
        return false;
      }
    };
  }

  /**
   * @param consumer Track information consumer that the track provider passes the raw packets to.
   * @return Track audio provider.
   */
  public MpegFileTrackProvider loadReader(MpegTrackConsumer consumer) {
    if (fragmentedFileReader.initialise(consumer)) {
      return fragmentedFileReader;
    } else if (standardFileReader.initialise(consumer)) {
      return standardFileReader;
    } else {
      return null;
    }
  }

  private void parseTrackInfo(MpegSectionInfo trak) throws IOException {
    final MpegTrackInfo.Builder trackInfo = new MpegTrackInfo.Builder();

    reader.in(trak).handleVersioned("tkhd", tkhd -> {
      reader.data.skipBytes(tkhd.version == 1 ? 16 : 8);

      trackInfo.setTrackId(reader.data.readInt());
    }).handle("mdia", mdia -> {
      reader.in(mdia).handleVersioned("hdlr", hdlr -> {
        reader.data.skipBytes(4);

        trackInfo.setHandler(reader.readFourCC());
      }).handleVersioned("mdhd", mdhd ->
          standardFileReader.readMediaHeaders(mdhd, trackInfo.getTrackId())
      ).handle("minf", minf -> {
        reader.in(minf).handle("stbl", stbl -> {
          MpegReader.Chain chain = reader.in(stbl);
          parseTrackCodecInfo(chain, trackInfo);
          standardFileReader.attachSampleTableParsers(chain, trackInfo.getTrackId());
          chain.run();
        }).run();
      }).run();
    }).run();

    tracks.add(trackInfo.build());
  }

  private void parseTrackCodecInfo(MpegReader.Chain chain, MpegTrackInfo.Builder trackInfo) {
    chain.handleVersioned("stsd", stsd -> {
      int entryCount = reader.data.readInt();
      if (entryCount > 0) {
        MpegSectionInfo codec = reader.nextChild(stsd);
        trackInfo.setCodecName(codec.type);

        if ("soun".equals(trackInfo.getHandler())) {
          parseSoundTrackCodec(codec, trackInfo);
        }
      }
    });
  }

  private void parseSoundTrackCodec(MpegSectionInfo codec, MpegTrackInfo.Builder trackInfo) throws IOException {
    reader.parseFlags(codec);

    reader.data.readUnsignedShort(); // data_reference_index
    reader.data.readUnsignedShort(); // apple: sound_version
    reader.data.skipBytes(8); // reserved

    trackInfo.setChannelCount(reader.data.readUnsignedShort());

    reader.data.readUnsignedShort(); // sample_size
    reader.data.readUnsignedShort(); // apple stuff

    trackInfo.setSampleRate(reader.data.readInt());
  }

  private void parseEventMessage(MpegSectionInfo emsg) throws IOException {
    reader.readTerminatedString(); // scheme_id_uri
    reader.readTerminatedString(); // value
    reader.data.readInt(); // timescale
    reader.data.readInt(); // presentation_time_delta
    reader.data.readInt(); // event_duration

    int remaining = (int) ((emsg.offset + emsg.length) - reader.seek.getPosition());

    lastEventMessage = new byte[remaining];
    reader.data.readFully(lastEventMessage);
  }
}
