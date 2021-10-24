package com.sedmelluq.discord.lavaplayer.source.youtube

import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools.throwWithDebugInfo
import com.sedmelluq.discord.lavaplayer.tools.json.JsonBrowser
import mu.KotlinLogging

data class YoutubeTrackJsonData(
    @JvmField val playerResponse: JsonBrowser,
    @JvmField val polymerArguments: JsonBrowser,
    @JvmField val playerScriptUrl: String?
) {
    companion object {
        private val log = KotlinLogging.logger { }

        @JvmStatic
        fun fromMainResult(result: JsonBrowser): YoutubeTrackJsonData {
            try {
                var playerInfo = JsonBrowser.NULL_BROWSER
                var playerResponse = JsonBrowser.NULL_BROWSER

                for (child in result.values()) {
                    if (child.isMap) {
                        if (playerInfo.isNull) {
                            playerInfo = child["player"]
                        }

                        if (playerResponse.isNull) {
                            playerResponse = child["playerResponse"]
                        }
                    } else {
                        if (playerResponse.isNull) {
                            playerResponse = result
                        }
                    }
                }

                if (!playerInfo.isNull) {
                    return fromPolymerPlayerInfo(playerInfo, playerResponse)
                } else if (!playerResponse.isNull) {
                    return YoutubeTrackJsonData(playerResponse, JsonBrowser.NULL_BROWSER, null)
                }
            } catch (e: Exception) {
                throw throwWithDebugInfo(log, e, "Error parsing result", "json", result.format())
            }

            throw throwWithDebugInfo(log, null, "Neither player nor playerResponse in result", "json", result.format())
        }

        private fun fromPolymerPlayerInfo(playerInfo: JsonBrowser, playerResponse: JsonBrowser): YoutubeTrackJsonData {
            val args = playerInfo["args"]
            val playerScriptUrl = playerInfo["assets"]["js"].text

            // In case of Polymer, the playerResponse with formats is the one embedded in args, NOT the one in outer JSON.
            // However, if no player_response is available, use the outer playerResponse.
            val playerResponseText = args["player_response"].text
                ?: return YoutubeTrackJsonData(playerResponse, args, playerScriptUrl)

            return YoutubeTrackJsonData(parsePlayerResponse(playerResponseText), args, playerScriptUrl)
        }

        private fun parsePlayerResponse(playerResponseText: String): JsonBrowser {
            try {
                return JsonBrowser.parse(playerResponseText)
            } catch (e: Exception) {
                throw throwWithDebugInfo(log, e, "Failed to parse player_response", "value", playerResponseText)
            }
        }

    }

    fun withPlayerScriptUrl(playerScriptUrl: String): YoutubeTrackJsonData {
        return YoutubeTrackJsonData(playerResponse, polymerArguments, playerScriptUrl)
    }
}
