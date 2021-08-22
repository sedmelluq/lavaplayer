package lavaplayer.source.youtube;

import lavaplayer.tools.http.ExtendedHttpConfigurable;
import lavaplayer.track.AudioItem;
import lavaplayer.track.AudioTrack;
import lavaplayer.track.AudioTrackInfo;

import java.util.function.Function;

public interface YoutubeSearchResultLoader {
    AudioItem loadSearchResult(String query, Function<AudioTrackInfo, AudioTrack> trackFactory);

    ExtendedHttpConfigurable getHttpConfiguration();
}
