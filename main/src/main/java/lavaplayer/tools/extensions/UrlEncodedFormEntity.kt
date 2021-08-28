package lavaplayer.tools.extensions

import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.message.BasicNameValuePair
import java.nio.charset.Charset

fun UrlEncodedFormEntity(vararg parameters: Pair<String, String>, charset: Charset) =
    UrlEncodedFormEntity(parameters.map { BasicNameValuePair(it.first, it.second) }, charset)
