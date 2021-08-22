package lavaplayer.source.soundcloud;

import lavaplayer.tools.io.HttpInterfaceManager;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.AudioTrackCollection;

import java.util.function.Function;

public interface SoundCloudPlaylistLoader {
    AudioTrackCollection load(
        String identifier,
        HttpInterfaceManager httpInterfaceManager,
        Function<AudioTrackInfo, AudioTrack> trackFactory
    );
}
