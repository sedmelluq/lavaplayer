package lavaplayer.source.soundcloud;

import lavaplayer.tools.JsonBrowser;
import lavaplayer.tools.io.HttpClientTools;
import lavaplayer.tools.io.HttpInterface;
import lavaplayer.tools.io.PersistentHttpStream;

import java.io.IOException;
import java.net.URI;

public class SoundCloudHelper {
    public static String nonMobileUrl(String url) {
        if (url.startsWith("https://m.")) {
            return "https://" + url.substring("https://m.".length());
        } else {
            return url;
        }
    }

    public static String loadPlaybackUrl(HttpInterface httpInterface, String jsonUrl) throws IOException {
        try (PersistentHttpStream stream = new PersistentHttpStream(httpInterface, URI.create(jsonUrl), null)) {
            if (!HttpClientTools.isSuccessWithContent(stream.checkStatusCode())) {
                throw new IOException("Invalid status code for soundcloud stream: " + stream.checkStatusCode());
            }

            JsonBrowser json = JsonBrowser.parse(stream);
            return json.get("url").text();
        }
    }
}
