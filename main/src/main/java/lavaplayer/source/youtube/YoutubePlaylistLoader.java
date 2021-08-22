package lavaplayer.source.youtube;

import lavaplayer.tools.io.HttpInterface;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;
import lavaplayer.track.AudioTrackCollection;

import java.util.function.Function;

public interface YoutubePlaylistLoader {
    void setPlaylistPageCount(int playlistPageCount);

    AudioTrackCollection load(HttpInterface httpInterface,
                              String playlistId,
                              String selectedVideoId,
                              Function<AudioTrackInfo, AudioTrack> trackFactory);
}
