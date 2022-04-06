package com.sedmelluq.discord.lavaplayer.source.youtube;

import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.discord.lavaplayer.tools.ExceptionTools.throwWithDebugInfo;
import static com.sedmelluq.discord.lavaplayer.tools.JsonBrowser.NULL_BROWSER;

public class YoutubeTrackJsonData {
  private static final Logger log = LoggerFactory.getLogger(DefaultYoutubeTrackDetailsLoader.class);

  public final JsonBrowser playerResponse;
  public final JsonBrowser polymerArguments;
  public final String playerScriptUrl;

  public YoutubeTrackJsonData(JsonBrowser playerResponse, JsonBrowser polymerArguments, String playerScriptUrl) {
    this.playerResponse = playerResponse;
    this.polymerArguments = polymerArguments;
    this.playerScriptUrl = playerScriptUrl;
  }

  public YoutubeTrackJsonData withPlayerScriptUrl(String playerScriptUrl) {
    return new YoutubeTrackJsonData(playerResponse, polymerArguments, playerScriptUrl);
  }

  public static YoutubeTrackJsonData fromMainResult(JsonBrowser result) {
    try {
      JsonBrowser playerInfo = NULL_BROWSER;
      JsonBrowser playerResponse = NULL_BROWSER;

      if (result.isMap()) {
          playerInfo = result.get("player");
          playerResponse = result.get("playerResponse");
      }
      
      for (JsonBrowser child : result.values()) {
        if (child.isMap()) {
          if (playerInfo.isNull()) {
            playerInfo = child.get("player");
          }

          if (playerResponse.isNull()) {
            playerResponse = child.get("playerResponse");
          }
        }
      }

      if (!playerInfo.isNull()) {
        return fromPolymerPlayerInfo(playerInfo, playerResponse);
      } else if (!playerResponse.isNull()) {
        return new YoutubeTrackJsonData(playerResponse, NULL_BROWSER, null);
      }
    } catch (Exception e) {
      throw throwWithDebugInfo(log, e, "Error parsing result", "json", result.format());
    }

    throw throwWithDebugInfo(log, null, "Neither player nor playerResponse in result", "json", result.format());
  }

  public static YoutubeTrackJsonData fromEmbedParts(JsonBrowser embedInfo, JsonBrowser videoInfo) {
    String playerScriptUrl = embedInfo.get("assets").get("js").text();
    String playerResponseText = videoInfo.get("player_response").text();

    if (playerResponseText == null) {
      String embeddedPlayerResponseText = embedInfo.get("args").get("embedded_player_response").text();
      return new YoutubeTrackJsonData(parsePlayerResponse(embeddedPlayerResponseText), videoInfo, playerScriptUrl);
    }

    return new YoutubeTrackJsonData(parsePlayerResponse(playerResponseText), videoInfo, playerScriptUrl);
  }

  private static YoutubeTrackJsonData fromPolymerPlayerInfo(JsonBrowser playerInfo, JsonBrowser playerResponse) {
    JsonBrowser args = playerInfo.get("args");
    String playerScriptUrl = playerInfo.get("assets").get("js").text();

    String playerResponseText = args.get("player_response").text();

    if (playerResponseText == null) {
      // In case of Polymer, the playerResponse with formats is the one embedded in args, NOT the one in outer JSON.
      // However, if no player_response is available, use the outer playerResponse.
      return new YoutubeTrackJsonData(playerResponse, args, playerScriptUrl);
    }

    return new YoutubeTrackJsonData(parsePlayerResponse(playerResponseText), args, playerScriptUrl);
  }

  private static JsonBrowser parsePlayerResponse(String playerResponseText) {
    try {
      return JsonBrowser.parse(playerResponseText);
    } catch (Exception e) {
      throw throwWithDebugInfo(log, e, "Failed to parse player_response", "value", playerResponseText);
    }
  }
}
