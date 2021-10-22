package lavaplayer.tools.extensions

import lavaplayer.tools.json.JsonTools
import org.apache.http.HttpEntity
import org.apache.http.util.EntityUtils
import java.io.InputStream

val linkPattern = "^https?://.+".toPattern()

fun String.isLink() =
    linkPattern.matcher(this).find()

inline fun <reified T : Any> String.decodeJson(): T =
    JsonTools.decode(this)

inline fun <reified T : Any> T.encodeJson(): String =
    JsonTools.encode(this)


