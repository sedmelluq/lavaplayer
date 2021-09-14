package lavaplayer.source.youtube

import lavaplayer.tools.FriendlyException
import lavaplayer.tools.http.HttpContextFilter
import lavaplayer.tools.io.HttpClientTools
import org.apache.commons.codec.digest.DigestUtils
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.BasicCookieStore

class YoutubeHttpContextFilter : HttpContextFilter {
    companion object {
        private const val ATTRIBUTE_RESET_RETRY = "isResetRetry"
        private var PAPISID = "HElVHkUVenb2eFXx/AhvhxMhD_KPsM4nZE"
        private var PSID = "8Qc_mMTGhpfQdTm1-fdKq6rh9KNCUC9OONEP44RAQkvVrQrFDkgjRaj6vJdchtNXMrWd4w."
        private var PSIDCC = "AJi4QfE9ix2TVKVWZzmswEkeDpCcZnuScw9N2pu2dS2fGx1Nyrtv_uDH4vvaiujL82_Ys1OO"
    }

    override fun onContextOpen(context: HttpClientContext) {
        var cookieStore = context.cookieStore
        if (cookieStore == null) {
            cookieStore = BasicCookieStore()
            context.cookieStore = cookieStore
        }

        // Reset cookies for each sequence of requests.
        cookieStore.clear()
    }

    override fun onContextClose(context: HttpClientContext) {}
    override fun onRequest(context: HttpClientContext, request: HttpUriRequest, isRepetition: Boolean) {
        if (!isRepetition) {
            context.removeAttribute(ATTRIBUTE_RESET_RETRY)
        }

        val millis = System.currentTimeMillis()
        val SAPISIDHASH = DigestUtils.sha1Hex("$millis $PAPISID ${YoutubeConstants.YOUTUBE_ORIGIN}")

        request.setHeader("Cookie", "__Secure-3PAPISID=$PAPISID __Secure-3PSID=$PSID __Secure-3PSIDCC=$PSIDCC")
        request.setHeader("Origin", YoutubeConstants.YOUTUBE_ORIGIN)
        request.setHeader("Authorization", "SAPISIDHASH ${millis}_$SAPISIDHASH")
    }

    override fun onRequestResponse(
        context: HttpClientContext,
        request: HttpUriRequest,
        response: HttpResponse
    ): Boolean {
        if (response.statusLine.statusCode == 429) {
            throw FriendlyException(
                "This IP address has been blocked by YouTube (429).",
                FriendlyException.Severity.COMMON,
                null
            )
        }

        return false
    }

    override fun onRequestException(context: HttpClientContext, request: HttpUriRequest, error: Throwable): Boolean {
        // Always retry once in case of connection reset exception.
        if (HttpClientTools.isConnectionResetException(error)) {
            if (context.getAttribute(ATTRIBUTE_RESET_RETRY) == null) {
                context.setAttribute(ATTRIBUTE_RESET_RETRY, true)
                return true
            }
        }

        return false
    }
}
