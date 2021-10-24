package com.sedmelluq.discord.lavaplayer.tools.extensions

import com.sedmelluq.discord.lavaplayer.tools.json.JsonTools
import java.io.InputStream

inline fun <reified T : Any> InputStream.decodeJson(): T =
    JsonTools.decode(this)
