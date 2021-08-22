package lavaplayer.source.youtube;

import lavaplayer.tools.io.HttpInterface;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.AudioTrackCollection;

import java.util.function.Function;

public interface YoutubeMixLoader {
    AudioTrackCollection load(
        HttpInterface httpInterface,
        String mixId,
        String selectedVideoId,
        Function<AudioTrackInfo, AudioTrack> trackFactory
    );
}
