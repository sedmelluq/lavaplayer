package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles parsing and caching of signature ciphers
 */
public class YoutubeSignatureCipherManager {
  private static final Logger log = LoggerFactory.getLogger(YoutubeSignatureCipherManager.class);

  private static final String VARIABLE_PART = "[a-zA-Z_\\$][a-zA-Z_0-9]*";
  private static final String VARIABLE_PART_DEFINE = "\\\"?" + VARIABLE_PART + "\\\"?";
  private static final String BEFORE_ACCESS = "(?:\\[\\\"|\\.)";
  private static final String AFTER_ACCESS = "(?:\\\"\\]|)";
  private static final String VARIABLE_PART_ACCESS = BEFORE_ACCESS + VARIABLE_PART + AFTER_ACCESS;
  private static final String REVERSE_PART = ":function\\(a\\)\\{(?:return )?a\\.reverse\\(\\)\\}";
  private static final String SLICE_PART = ":function\\(a,b\\)\\{return a\\.slice\\(b\\)\\}";
  private static final String SPLICE_PART = ":function\\(a,b\\)\\{a\\.splice\\(0,b\\)\\}";
  private static final String SWAP_PART = ":function\\(a,b\\)\\{" +
      "var c=a\\[0\\];a\\[0\\]=a\\[b%a\\.length\\];a\\[b(?:%a.length|)\\]=c(?:;return a)?\\}";

  private static final Pattern functionPattern = Pattern.compile("" +
      "function(?: " + VARIABLE_PART + ")?\\(a\\)\\{" +
      "a=a\\.split\\(\"\"\\);\\s*" +
      "((?:(?:a=)?" + VARIABLE_PART + VARIABLE_PART_ACCESS + "\\(a,\\d+\\);)+)" +
      "return a\\.join\\(\"\"\\)" +
      "\\}"
  );

  private static final Pattern actionsPattern = Pattern.compile("" +
      "var (" + VARIABLE_PART + ")=\\{((?:(?:" +
      VARIABLE_PART_DEFINE + REVERSE_PART + "|" +
      VARIABLE_PART_DEFINE + SLICE_PART + "|" +
      VARIABLE_PART_DEFINE + SPLICE_PART + "|" +
      VARIABLE_PART_DEFINE + SWAP_PART +
      "),?\\n?)+)\\};"
  );

  private static final String PATTERN_PREFIX = "(?:^|,)\\\"?(" + VARIABLE_PART + ")\\\"?";

  private static final Pattern reversePattern = Pattern.compile(PATTERN_PREFIX + REVERSE_PART, Pattern.MULTILINE);
  private static final Pattern slicePattern = Pattern.compile(PATTERN_PREFIX + SLICE_PART, Pattern.MULTILINE);
  private static final Pattern splicePattern = Pattern.compile(PATTERN_PREFIX + SPLICE_PART, Pattern.MULTILINE);
  private static final Pattern swapPattern = Pattern.compile(PATTERN_PREFIX + SWAP_PART, Pattern.MULTILINE);

  private static final Pattern signatureExtraction = Pattern.compile("/s/([^/]+)/");

  private final ConcurrentMap<String, YoutubeSignatureCipher> cipherCache;
  private final Set<String> dumpedScriptUrls;
  private final Object cipherLoadLock;

  /**
   * Create a new signature cipher manager
   */
  public YoutubeSignatureCipherManager() {
    this.cipherCache = new ConcurrentHashMap<>();
    this.dumpedScriptUrls = new HashSet<>();
    this.cipherLoadLock = new Object();
  }

  /**
   * Produces a valid playback URL for the specified track
   * @param httpInterface HTTP interface to use
   * @param playerScript Address of the script which is used to decipher signatures
   * @param format The track for which to get the URL
   * @return Valid playback URL
   * @throws IOException On network IO error
   */
  public URI getValidUrl(HttpInterface httpInterface, String playerScript, YoutubeTrackFormat format) throws IOException {
    String signature = format.getSignature();
    URI initialUrl = format.getUrl();

    if (signature == null) {
      return initialUrl;
    }

    YoutubeSignatureCipher cipher = getCipherKeyFromScript(httpInterface, playerScript);

    try {
      return new URIBuilder(initialUrl)
          .setParameter("ratebypass", "yes")
          .setParameter("signature", cipher.apply(signature))
          .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Produces a valid dash XML URL from the possibly ciphered URL.
   * @param httpInterface HTTP interface instance to use
   * @param playerScript Address of the script which is used to decipher signatures
   * @param dashUrl URL of the dash XML, possibly with a ciphered signature
   * @return Valid dash XML URL
   * @throws IOException On network IO error
   */
  public String getValidDashUrl(HttpInterface httpInterface, String playerScript, String dashUrl) throws IOException {
    Matcher matcher = signatureExtraction.matcher(dashUrl);

    if (!matcher.find()) {
      return dashUrl;
    }

    YoutubeSignatureCipher cipher = getCipherKeyFromScript(httpInterface, playerScript);
    return matcher.replaceFirst("/signature/" + cipher.apply(matcher.group(1)) + "/");
  }

  private YoutubeSignatureCipher getCipherKeyFromScript(HttpInterface httpInterface, String cipherScriptUrl) throws IOException {
    YoutubeSignatureCipher cipherKey = cipherCache.get(cipherScriptUrl);

    if (cipherKey == null) {
      synchronized (cipherLoadLock) {
        log.debug("Parsing cipher from player script {}.", cipherScriptUrl);

        try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(parseTokenScriptUrl(cipherScriptUrl)))) {
          validateResponseCode(response);

          cipherKey = extractTokensFromScript(IOUtils.toString(response.getEntity().getContent(), "UTF-8"), cipherScriptUrl);
          cipherCache.put(cipherScriptUrl, cipherKey);
        }
      }
    }

    return cipherKey;
  }

  private void validateResponseCode(CloseableHttpResponse response) throws IOException {
    int statusCode = response.getStatusLine().getStatusCode();

    if (statusCode != 200) {
      throw new IOException("Received non-success response code " + statusCode);
    }
  }

  private List<String> getQuotedFunctions(String... functionNames) {
    return Stream.of(functionNames)
        .filter(Objects::nonNull)
        .map(Pattern::quote)
        .collect(Collectors.toList());
  }

  private void dumpProblematicScript(String script, String sourceUrl, String issue) {
    if (!dumpedScriptUrls.add(sourceUrl)) {
      return;
    }

    try {
      Path path = Files.createTempFile("lavaplayer-yt-player-script", ".js");
      Files.write(path, script.getBytes(StandardCharsets.UTF_8));

      log.error("Problematic YouTube player script {} detected (issue detected with script: {}). Dumped to {}.",
          sourceUrl, issue, path.toAbsolutePath());
    } catch (Exception e) {
      log.error("Failed to dump problematic YouTube player script {} (issue detected with script: {})", sourceUrl, issue);
    }
  }

  private YoutubeSignatureCipher extractTokensFromScript(String script, String sourceUrl) {
    Matcher actions = actionsPattern.matcher(script);
    if (!actions.find()) {
      dumpProblematicScript(script, sourceUrl, "no actions match");
      throw new IllegalStateException("Must find action functions from script: " + sourceUrl);
    }

    String actionBody = actions.group(2);

    String reverseKey = extractDollarEscapedFirstGroup(reversePattern, actionBody);
    String slicePart = extractDollarEscapedFirstGroup(slicePattern, actionBody);
    String splicePart = extractDollarEscapedFirstGroup(splicePattern, actionBody);
    String swapKey = extractDollarEscapedFirstGroup(swapPattern, actionBody);

    Pattern extractor = Pattern.compile(
        "(?:a=)?" + Pattern.quote(actions.group(1)) + BEFORE_ACCESS + "(" +
        String.join("|", getQuotedFunctions(reverseKey, slicePart, splicePart, swapKey)) +
        ")" + AFTER_ACCESS + "\\(a,(\\d+)\\)"
    );

    Matcher functions = functionPattern.matcher(script);
    if (!functions.find()) {
      dumpProblematicScript(script, sourceUrl, "no decipher function match");
      throw new IllegalStateException("Must find decipher function from script.");
    }

    Matcher matcher = extractor.matcher(functions.group(1));

    YoutubeSignatureCipher cipherKey = new YoutubeSignatureCipher();

    while (matcher.find()) {
      String type = matcher.group(1);

      if (type.equals(swapKey)) {
        cipherKey.addOperation(new YoutubeCipherOperation(YoutubeCipherOperationType.SWAP, Integer.parseInt(matcher.group(2))));
      } else if (type.equals(reverseKey)) {
        cipherKey.addOperation(new YoutubeCipherOperation(YoutubeCipherOperationType.REVERSE, 0));
      } else if (type.equals(slicePart)) {
        cipherKey.addOperation(new YoutubeCipherOperation(YoutubeCipherOperationType.SLICE, Integer.parseInt(matcher.group(2))));
      } else if (type.equals(splicePart)) {
        cipherKey.addOperation(new YoutubeCipherOperation(YoutubeCipherOperationType.SPLICE, Integer.parseInt(matcher.group(2))));
      } else {
        dumpProblematicScript(script, sourceUrl, "unknown cipher operation found");
      }
    }

    if (cipherKey.isEmpty()) {
      log.error("No operations detected from cipher extracted from {}.", sourceUrl);
      dumpProblematicScript(script, sourceUrl, "no cipher operations");
    }

    return cipherKey;
  }

  private static String extractDollarEscapedFirstGroup(Pattern pattern, String text) {
    Matcher matcher = pattern.matcher(text);
    return matcher.find() ? matcher.group(1).replace("$", "\\$") : null;
  }

  private static URI parseTokenScriptUrl(String urlString) {
    try {
      if (urlString.startsWith("//")) {
        return new URI("https:" + urlString);
      } else if (urlString.startsWith("/")) {
        return new URI("https://s.ytimg.com" + urlString);
      } else {
        return new URI(urlString);
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
