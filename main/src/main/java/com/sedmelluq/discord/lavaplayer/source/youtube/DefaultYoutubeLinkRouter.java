package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;

public class DefaultYoutubeLinkRouter implements YoutubeLinkRouter {
  private static final String SEARCH_PREFIX = "ytsearch:";
  private static final String SEARCH_MUSIC_PREFIX = "ytmsearch:";

  private static final String PROTOCOL_REGEX = "(?:http://|https://|)";
  private static final String DOMAIN_REGEX = "(?:www\\.|m\\.|music\\.|)youtube\\.com";
  private static final String SHORT_DOMAIN_REGEX = "(?:www\\.|)youtu\\.be";
  private static final String VIDEO_ID_REGEX = "(?<v>[a-zA-Z0-9_-]{11})";
  private static final String PLAYLIST_ID_REGEX = "(?<list>(PL|LL|FL|UU)[a-zA-Z0-9_-]+)";

  private static final Pattern directVideoIdPattern = Pattern.compile("^" + VIDEO_ID_REGEX + "$");

  private final Extractor[] extractors = new Extractor[] {
      new Extractor(directVideoIdPattern, Routes::track),
      new Extractor(Pattern.compile("^" + PLAYLIST_ID_REGEX + "$"), this::routeDirectPlaylist),
      new Extractor(Pattern.compile("^" + PROTOCOL_REGEX + DOMAIN_REGEX + "/.*"), this::routeFromMainDomain),
      new Extractor(Pattern.compile("^" + PROTOCOL_REGEX + SHORT_DOMAIN_REGEX + "/.*"), this::routeFromShortDomain)
  };

  @Override
  public <T> T route(String link, Routes<T> routes) {
    if (link.startsWith(SEARCH_PREFIX)) {
      return routes.search(link.substring(SEARCH_PREFIX.length()).trim());
    } else if (link.startsWith(SEARCH_MUSIC_PREFIX)) {
      return routes.searchMusic(link.substring(SEARCH_MUSIC_PREFIX.length()).trim());
    }

    for (Extractor extractor : extractors) {
      if (extractor.pattern.matcher(link).matches()) {
        T item = extractor.router.extract(routes, link);

        if (item != null) {
          return item;
        }
      }
    }

    return null;
  }

  protected <T> T routeDirectPlaylist(Routes<T> routes, String id) {
    return routes.playlist(id, null);
  }

  protected <T> T routeFromMainDomain(Routes<T> routes, String url) {
    UrlInfo urlInfo = getUrlInfo(url, true);

    if ("/watch".equals(urlInfo.path)) {
      String videoId = urlInfo.parameters.get("v");

      if (videoId != null) {
        return routeFromUrlWithVideoId(routes, videoId, urlInfo);
      }
    } else if ("/playlist".equals(urlInfo.path)) {
      String playlistId = urlInfo.parameters.get("list");

      if (playlistId != null) {
        return routes.playlist(playlistId, null);
      }
    } else if ("/watch_videos".equals(urlInfo.path)) {
      String videoIds = urlInfo.parameters.get("video_ids");
      if (videoIds != null) {
        return routes.anonymous(videoIds);
      }
    }

    return null;
  }

  protected <T> T routeFromUrlWithVideoId(Routes<T> routes, String videoId, UrlInfo urlInfo) {
    if (videoId.length() > 11) {
      // YouTube allows extra junk in the end, it redirects to the correct video.
      videoId = videoId.substring(0, 11);
    }

    if (!directVideoIdPattern.matcher(videoId).matches()) {
      return routes.none();
    } else if (urlInfo.parameters.containsKey("list")) {
      String playlistId = urlInfo.parameters.get("list");

      if (playlistId.startsWith("RD")) {
        return routes.mix(playlistId, videoId);
      } else {
        return routes.playlist(urlInfo.parameters.get("list"), videoId);
      }
    } else {
      return routes.track(videoId);
    }
  }

  protected <T> T routeFromShortDomain(Routes<T> routes, String url) {
    UrlInfo urlInfo = getUrlInfo(url, true);
    return routeFromUrlWithVideoId(routes, urlInfo.path.substring(1), urlInfo);
  }

  private static UrlInfo getUrlInfo(String url, boolean retryValidPart) {
    try {
      if (!url.startsWith("http://") && !url.startsWith("https://")) {
        url = "https://" + url;
      }

      URIBuilder builder = new URIBuilder(url);
      return new UrlInfo(builder.getPath(), builder.getQueryParams().stream()
          .filter(it -> it.getValue() != null)
          .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue, (a, b) -> a)));
    } catch (URISyntaxException e) {
      if (retryValidPart) {
        return getUrlInfo(url.substring(0, e.getIndex() - 1), false);
      } else {
        throw new FriendlyException("Not a valid URL: " + url, COMMON, e);
      }
    }
  }

  private static class UrlInfo {
    private final String path;
    private final Map<String, String> parameters;

    private UrlInfo(String path, Map<String, String> parameters) {
      this.path = path;
      this.parameters = parameters;
    }
  }

  private static class Extractor {
    private final Pattern pattern;
    private final ExtractorRouter router;

    private Extractor(Pattern pattern, ExtractorRouter router) {
      this.pattern = pattern;
      this.router = router;
    }
  }

  private interface ExtractorRouter {
    <T> T extract(Routes<T> routes, String url);
  }
}
