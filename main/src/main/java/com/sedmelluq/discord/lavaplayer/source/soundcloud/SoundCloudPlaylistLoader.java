package com.sedmelluq.discord.lavaplayer.source.soundcloud;

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.util.function.Function;

public interface SoundCloudPlaylistLoader {
    AudioPlaylist load(
        String identifier,
        HttpInterfaceManager httpInterfaceManager,
        Function<AudioTrackInfo, AudioTrack> trackFactory
    );
}
