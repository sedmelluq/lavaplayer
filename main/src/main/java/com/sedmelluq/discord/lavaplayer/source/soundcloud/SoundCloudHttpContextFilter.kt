package com.sedmelluq.discord.lavaplayer.source.soundcloud

import com.sedmelluq.discord.lavaplayer.tools.http.HttpContextFilter
import com.sedmelluq.discord.lavaplayer.tools.http.HttpContextRetryCounter
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.client.utils.URIBuilder
import java.net.URISyntaxException

class SoundCloudHttpContextFilter(private val clientIdTracker: SoundCloudClientIdTracker) : HttpContextFilter {
    override fun onContextOpen(context: HttpClientContext) {}

    override fun onContextClose(context: HttpClientContext) {}

    override fun onRequest(context: HttpClientContext, request: HttpUriRequest, isRepetition: Boolean) {
        retryCounter.handleUpdate(context, isRepetition)
        if (clientIdTracker.isIdFetchContext(context)) {
            // Used for fetching client ID, let's not recurse.
            return
        } else if (request.uri.host.contains("sndcdn.com")) {
            // CDN urls do not require client ID (it actually breaks them)
            return
        }

        try {
            val uri = URIBuilder(request.uri)
                .setParameter("client_id", clientIdTracker.clientId)
                .build()

            if (request is HttpRequestBase) {
                request.uri = uri
            } else {
                throw IllegalStateException("Cannot update request URI.")
            }
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }

    override fun onRequestResponse(
        context: HttpClientContext,
        request: HttpUriRequest,
        response: HttpResponse
    ): Boolean {
        return if (clientIdTracker.isIdFetchContext(context) || retryCounter.retryCountFor(context) >= 1) {
            false
        } else if (response.statusLine.statusCode == HttpStatus.SC_UNAUTHORIZED) {
            clientIdTracker.updateClientId()
            true
        } else {
            false
        }
    }

    override fun onRequestException(context: HttpClientContext, request: HttpUriRequest, error: Throwable): Boolean {
        return false
    }

    companion object {
        private val retryCounter = HttpContextRetryCounter("sc-id-retry")
    }
}
