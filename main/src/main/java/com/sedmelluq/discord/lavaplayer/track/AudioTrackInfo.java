package com.sedmelluq.discord.lavaplayer.track;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Meta info for an audio track
 */
public class AudioTrackInfo {
    /**
     * Track title
     */
    public final String title;

    /**
     * Track author, if known
     */
    public final String author;

    /**
     * Length of the track in milliseconds, UnitConstants.DURATION_MS_UNKNOWN for streams
     */
    public final long length;

    /**
     * Audio source specific track identifier
     */
    public final String identifier;

    /**
     * True if this track is a stream
     */
    public final boolean isStream;

    /**
     * URL of the track, or local path to the file.
     */
    public final String uri;

    /**
     * URL of the artwork for this track.
     */
    public final String artworkUrl;

    /**
     * @param title      Track title
     * @param author     Track author, if known
     * @param length     Length of the track in milliseconds
     * @param identifier Audio source specific track identifier
     * @param isStream   True if this track is a stream
     * @param uri        URL of the track or path to its file.
     */
    public AudioTrackInfo(String title, String author, long length, String identifier, boolean isStream, String uri, String artworkUrl) {
        this.title = title;
        this.author = author;
        this.length = length;
        this.identifier = identifier;
        this.isStream = isStream;
        this.uri = uri;
        this.artworkUrl = artworkUrl;
    }

    static public int getVersion(MessageInput stream, DataInput input) throws IOException {
        return (stream.getMessageFlags() & DefaultAudioPlayerManager.TRACK_INFO_VERSIONED) != 0 ? (input.readByte() & 0xFF) : 1;
    }

    static public void encode(DataOutput output, AudioTrackInfo info) throws IOException {
        output.writeUTF(info.title);
        output.writeUTF(info.author);
        output.writeLong(info.length);
        output.writeUTF(info.identifier);
        output.writeBoolean(info.isStream);
        DataFormatTools.writeNullableText(output, info.uri);
        DataFormatTools.writeNullableText(output, info.artworkUrl);
    }

    static public AudioTrackInfo decode(DataInput input, int version) throws IOException {
        return new AudioTrackInfo(
            input.readUTF(),
            input.readUTF(),
            input.readLong(),
            input.readUTF(),
            input.readBoolean(),
            version >= 2 ? DataFormatTools.readNullableText(input) : null,
            DataFormatTools.readNullableText(input)
        );
    }

}
