package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.util.function.Function;

public interface YoutubePlaylistLoader {
    void setPlaylistPageCount(int playlistPageCount);

    AudioPlaylist load(HttpInterface httpInterface, String playlistId, String selectedVideoId,
                       Function<AudioTrackInfo, AudioTrack> trackFactory);
}
