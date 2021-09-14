package lavaplayer.tools.extensions

import lavaplayer.tools.json.JsonTools
import org.apache.commons.io.IOUtils
import org.apache.http.HttpEntity

val linkPattern = "^https?://.+".toPattern()

fun String.isLink() =
    linkPattern.matcher(this).find()

inline fun <reified T : Any> String.decodeJson(): T =
    JsonTools.decode(this)

inline fun <reified T : Any> HttpEntity.decodeJson(): T =
    JsonTools.decode(IOUtils.toString(content, "UTF-8"))


