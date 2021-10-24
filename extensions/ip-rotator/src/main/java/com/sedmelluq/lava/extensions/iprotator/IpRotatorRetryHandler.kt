package com.sedmelluq.lava.extensions.iprotator

import org.apache.http.client.HttpRequestRetryHandler
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler
import org.apache.http.protocol.HttpContext
import java.io.IOException
import java.net.BindException
import java.net.SocketException

class IpRotatorRetryHandler : HttpRequestRetryHandler {
    override fun retryRequest(exception: IOException, executionCount: Int, context: HttpContext): Boolean {
        if (exception is BindException) {
            return false
        } else if (exception is SocketException) {
            val message = exception.message
            if (message != null && message.contains("Protocol family unavailable")) {
                return false
            }
        }

        return DefaultHttpRequestRetryHandler.INSTANCE.retryRequest(exception, executionCount, context)
    }
}
