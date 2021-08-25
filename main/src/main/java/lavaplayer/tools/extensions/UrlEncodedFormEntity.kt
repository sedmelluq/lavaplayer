package lavaplayer.tools.extensions

import java.nio.charset.Charset
import org.apache.http.message.BasicNameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity

fun UrlEncodedFormEntity(vararg parameters: Pair<String, String>, charset: Charset) =
    UrlEncodedFormEntity(parameters.map { BasicNameValuePair(it.first, it.second) }, charset)
