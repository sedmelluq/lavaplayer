package com.sedmelluq.discord.lavaplayer.source.youtube.format;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeSignatureResolver;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackFormat;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackJsonData;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;

import java.util.List;

public interface OfflineYoutubeTrackFormatExtractor extends YoutubeTrackFormatExtractor {
    List<YoutubeTrackFormat> extract(YoutubeTrackJsonData data);

    @Override
    default List<YoutubeTrackFormat> extract(
        YoutubeTrackJsonData data,
        HttpInterface httpInterface,
        YoutubeSignatureResolver signatureResolver
    ) {
        return extract(data);
    }
}
