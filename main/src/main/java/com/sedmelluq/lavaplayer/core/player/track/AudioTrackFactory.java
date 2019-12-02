package com.sedmelluq.lavaplayer.core.player.track;

import com.sedmelluq.lavaplayer.core.player.configuration.AudioConfiguration;
import com.sedmelluq.lavaplayer.core.player.track.AudioTrackRequest;
import com.sedmelluq.lavaplayer.core.player.track.ExecutableAudioTrack;
import java.io.Closeable;

public interface AudioTrackFactory extends Closeable {
  ExecutableAudioTrack create(AudioTrackRequest request, AudioConfiguration configuration);
}
