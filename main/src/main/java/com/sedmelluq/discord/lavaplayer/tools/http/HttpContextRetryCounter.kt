package com.sedmelluq.discord.lavaplayer.tools.http

import org.apache.http.client.protocol.HttpClientContext

class HttpContextRetryCounter(private val attributeName: String) {
    fun handleUpdate(context: HttpClientContext, isRepetition: Boolean) {
        if (isRepetition) {
            context.retryCount++
        } else {
            context.retryCount = 0
        }
    }

    var HttpClientContext.retryCount
        get() = getAttribute(attributeName, RetryCount::class.java)?.value ?: 0
        set(value) = setAttribute(attributeName, RetryCount(value))

    data class RetryCount(var value: Int)
}
