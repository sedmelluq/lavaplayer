package lavaplayer.tools.http;

import lavaplayer.tools.io.HttpConfigurable;

interface ExtendedHttpConfigurable : HttpConfigurable {
    fun setHttpContextFilter(filter: HttpContextFilter)
}
