package com.sedmelluq.discord.lavaplayer.container.mpegts;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerProbe;
import com.sedmelluq.discord.lavaplayer.container.adts.AdtsStreamReader;
import com.sedmelluq.discord.lavaplayer.tools.io.SavedHeadSeekableInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult.supportedFormat;
import static com.sedmelluq.discord.lavaplayer.container.mpegts.MpegTsElementaryInputStream.ADTS_ELEMENTARY_STREAM;

public class MpegAdtsContainerProbe implements MediaContainerProbe {
    private static final Logger log = LoggerFactory.getLogger(MpegAdtsContainerProbe.class);

    @Override
    public String getName() {
        return "mpegts-adts";
    }

    @Override
    public boolean matchesHints(MediaContainerHints hints) {
        return "ts".equalsIgnoreCase(hints.fileExtension);
    }

    @Override
    public MediaContainerDetectionResult probe(AudioReference reference, SeekableInputStream inputStream)
        throws IOException {

        SavedHeadSeekableInputStream head = inputStream instanceof SavedHeadSeekableInputStream ?
            (SavedHeadSeekableInputStream) inputStream : null;

        if (head != null) {
            head.setAllowDirectReads(false);
        }

        MpegTsElementaryInputStream tsStream = new MpegTsElementaryInputStream(inputStream, ADTS_ELEMENTARY_STREAM);
        PesPacketInputStream pesStream = new PesPacketInputStream(tsStream);
        AdtsStreamReader reader = new AdtsStreamReader(pesStream);

        try {
            if (reader.findPacketHeader() != null) {
                log.debug("Track {} is an MPEG-TS stream with an ADTS track.", reference.identifier);

                return supportedFormat(this, null,
                    AudioTrackInfoBuilder.create(reference, inputStream)
                        .apply(tsStream.getLoadedMetadata())
                        .build()
                );
            }
        } catch (IndexOutOfBoundsException ignored) {
            // TS stream read too far and still did not find required elementary stream - SavedHeadSeekableInputStream throws
            // this because we disabled reads past the loaded "head".
        } finally {
            if (head != null) {
                head.setAllowDirectReads(true);
            }
        }

        return null;
    }

    @Override
    public AudioTrack createTrack(String parameters, AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
        return new MpegAdtsAudioTrack(trackInfo, inputStream);
    }
}
