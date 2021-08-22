package lavaplayer.source.youtube;

import lavaplayer.tools.io.HttpInterface;
import lavaplayer.track.AudioTrackInfo;

import java.util.List;

public interface YoutubeTrackDetails {
    AudioTrackInfo getTrackInfo();

    List<YoutubeTrackFormat> getFormats(HttpInterface httpInterface, YoutubeSignatureResolver signatureResolver);

    String getPlayerScript();
}
