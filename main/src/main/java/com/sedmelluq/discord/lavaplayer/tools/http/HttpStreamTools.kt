package com.sedmelluq.discord.lavaplayer.tools.http

import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpUriRequest

import java.io.IOException
import java.io.InputStream

object HttpStreamTools {
    @JvmStatic
    fun streamContent( httpInterface: HttpInterface, request: HttpUriRequest): InputStream {
        var response: CloseableHttpResponse? = null
        var success = false

        try {
            response = httpInterface.execute(request)
            val statusCode = response.statusLine.statusCode

            if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw IOException("Invalid status code from ${request.uri} URL: $statusCode")
            }

            success = true
            return response.entity.content
        } catch (e: IOException) {
            throw RuntimeException(e)
        } finally {
            if (response != null && !success) {
                ExceptionTools.closeWithWarnings(response)
            }
        }
    }
}
