package com.sedmelluq.discord.lavaplayer.tools.http

import org.apache.http.HttpClientConnection
import org.apache.http.HttpHost
import org.apache.http.config.ConnectionConfig
import org.apache.http.config.SocketConfig
import org.apache.http.conn.*
import org.apache.http.conn.routing.HttpRoute
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory
import org.apache.http.protocol.HttpContext
import java.io.IOException
import java.util.concurrent.TimeUnit

class SimpleHttpClientConnectionManager(
    private val connectionOperator: HttpClientConnectionOperator,
    val connectionFactory: HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> = ManagedHttpClientConnectionFactory.INSTANCE
) : HttpClientConnectionManager {
    @Volatile
    var socketConfig: SocketConfig = SocketConfig.DEFAULT

    @Volatile
    var connectionConfig: ConnectionConfig = ConnectionConfig.DEFAULT

    override fun requestConnection(route: HttpRoute, state: Any?): ConnectionRequest {
        return object : ConnectionRequest {

            override fun cancel(): Boolean {
                // Nothing to do.
                return false
            }

            override fun get(timeout: Long, timeUnit: TimeUnit): HttpClientConnection? {
                return connectionFactory.create(route, connectionConfig)
            }
        }
    }

    override fun releaseConnection(
        connection: HttpClientConnection,
        newState: Any?,
        validDuration: Long,
        timeUnit: TimeUnit
    ) {
        try {
            connection.close()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class)
    override fun connect(
        connection: HttpClientConnection,
        route: HttpRoute,
        connectTimeout: Int,
        context: HttpContext
    ) {
        val host: HttpHost = if (route.proxyHost != null) route.proxyHost else route.targetHost
        val managed = connection as ManagedHttpClientConnection

        connectionOperator.connect(managed, host, route.localSocketAddress, connectTimeout, socketConfig, context)
    }

    @Throws(IOException::class)
    override fun upgrade(connection: HttpClientConnection, route: HttpRoute, context: HttpContext) {
        this.connectionOperator.upgrade(connection as ManagedHttpClientConnection, route.targetHost, context)
    }

    override fun routeComplete(conn: HttpClientConnection, route: HttpRoute, context: HttpContext) {
        // Nothing to do.
    }

    override fun closeIdleConnections(idletime: Long, timeUnit: TimeUnit) {
        // Nothing to do.
    }

    override fun closeExpiredConnections() {
        // Nothing to do.
    }

    override fun shutdown() {
        // Nothing to do.
    }
}
