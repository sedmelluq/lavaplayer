package lavaplayer.source.youtube;

import lavaplayer.tools.io.HttpInterface;

public interface YoutubeTrackDetailsLoader {
    YoutubeTrackDetails loadDetails(HttpInterface httpInterface, String videoId, boolean requireFormats);
}
