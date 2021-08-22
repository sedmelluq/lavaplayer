package lavaplayer.source.soundcloud;

import lavaplayer.tools.JsonBrowser;
import lavaplayer.tools.io.HttpInterface;

import java.io.IOException;

public interface SoundCloudHtmlDataLoader {
    JsonBrowser load(HttpInterface httpInterface, String url) throws IOException;
}
