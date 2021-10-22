package lavaplayer.tools.extensions

import lavaplayer.tools.json.JsonTools
import java.io.InputStream

inline fun <reified T : Any> InputStream.decodeJson(): T =
    JsonTools.decode(this)
