package com.sedmelluq.discord.lavaplayer.source.youtube.format;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeSignatureResolver;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackFormat;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackJsonData;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;

import java.util.List;

public interface YoutubeTrackFormatExtractor {
    String DEFAULT_SIGNATURE_KEY = "signature";

    List<YoutubeTrackFormat> extract(
        YoutubeTrackJsonData response,
        HttpInterface httpInterface,
        YoutubeSignatureResolver signatureResolver
    );
}
