package com.sedmelluq.discord.lavaplayer.container;

import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

public class MediaContainerDescriptor {
    public final MediaContainerProbe probe;
    public final String parameters;

    public MediaContainerDescriptor(MediaContainerProbe probe, String parameters) {
        this.probe = probe;
        this.parameters = parameters;
    }

    public AudioTrack createTrack(AudioTrackInfo trackInfo, SeekableInputStream inputStream) {
        return probe.createTrack(parameters, trackInfo, inputStream);
    }
}
