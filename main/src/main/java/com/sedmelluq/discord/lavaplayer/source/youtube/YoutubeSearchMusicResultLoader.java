package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.http.ExtendedHttpConfigurable;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.util.function.Function;

public interface YoutubeSearchMusicResultLoader {
    AudioItem loadSearchMusicResult(String query, Function<AudioTrackInfo, AudioTrack> trackFactory);

    ExtendedHttpConfigurable getHttpConfiguration();
}
