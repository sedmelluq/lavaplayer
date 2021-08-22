package lavaplayer.source.soundcloud;

import lavaplayer.tools.DataFormatTools;
import lavaplayer.tools.DataFormatTools.TextRange;
import lavaplayer.tools.ExceptionTools;
import lavaplayer.tools.FriendlyException;
import lavaplayer.tools.JsonBrowser;
import lavaplayer.tools.io.HttpClientTools;
import lavaplayer.tools.io.HttpInterface;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

public class DefaultSoundCloudHtmlDataLoader implements SoundCloudHtmlDataLoader {
    private static final Logger log = LoggerFactory.getLogger(DefaultSoundCloudHtmlDataLoader.class);

    private static final TextRange[] JSON_RANGES = {
        new TextRange("window.__sc_hydration =", ";</script>"),
        new TextRange("catch(e){}})},", ");</script>"),
        new TextRange("){}})},", ");</script>")
    };

    @Override
    public JsonBrowser load(HttpInterface httpInterface, String url) throws IOException {
        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(url))) {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return JsonBrowser.NULL_BROWSER;
            }

            HttpClientTools.assertSuccessWithContent(response, "video page response");

            String html = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            String rootData = DataFormatTools.extractBetween(html, JSON_RANGES);

            if (rootData == null) {
                throw new FriendlyException("This url does not appear to be a playable track.", SUSPICIOUS,
                    ExceptionTools.throwWithDebugInfo(log, null, "No track JSON found", "html", html));
            }

            return JsonBrowser.parse(rootData);
        }
    }
}
