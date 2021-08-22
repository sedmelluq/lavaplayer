package com.sedmelluq.discord.lavaplayer.tools.http;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpHost;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.*;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class SimpleHttpClientConnectionManager implements HttpClientConnectionManager {
    private final HttpClientConnectionOperator connectionOperator;
    private final HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connectionFactory;
    private volatile SocketConfig socketConfig = SocketConfig.DEFAULT;
    private volatile ConnectionConfig connectionConfig = ConnectionConfig.DEFAULT;

    public SimpleHttpClientConnectionManager(
        HttpClientConnectionOperator connectionOperator,
        HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> factory
    ) {
        this.connectionOperator = connectionOperator;
        this.connectionFactory = factory != null ? factory : ManagedHttpClientConnectionFactory.INSTANCE;
    }

    public void setSocketConfig(SocketConfig config) {
        this.socketConfig = config;
    }

    public void setConnectionConfig(ConnectionConfig config) {
        this.connectionConfig = config;
    }

    @Override
    public ConnectionRequest requestConnection(HttpRoute route, Object state) {
        return new ConnectionRequest() {

            @Override
            public boolean cancel() {
                // Nothing to do.
                return false;
            }

            @Override
            public HttpClientConnection get(final long timeout, final TimeUnit timeUnit) {
                return connectionFactory.create(route, connectionConfig);
            }
        };
    }

    @Override
    public void releaseConnection(
        HttpClientConnection connection,
        Object newState,
        long validDuration,
        TimeUnit timeUnit
    ) {
        try {
            connection.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void connect(
        HttpClientConnection connection,
        HttpRoute route,
        int connectTimeout,
        HttpContext context
    ) throws IOException {
        HttpHost host;

        if (route.getProxyHost() != null) {
            host = route.getProxyHost();
        } else {
            host = route.getTargetHost();
        }

        InetSocketAddress localAddress = route.getLocalSocketAddress();

        ManagedHttpClientConnection managed = (ManagedHttpClientConnection) connection;
        this.connectionOperator.connect(managed, host, localAddress, connectTimeout, this.socketConfig, context);
    }

    @Override
    public void upgrade(HttpClientConnection connection, HttpRoute route, HttpContext context) throws IOException {
        ManagedHttpClientConnection managed = (ManagedHttpClientConnection) connection;
        this.connectionOperator.upgrade(managed, route.getTargetHost(), context);
    }

    @Override
    public void routeComplete(HttpClientConnection conn, HttpRoute route, HttpContext context) {
        // Nothing to do.
    }

    @Override
    public void closeIdleConnections(long idletime, TimeUnit timeUnit) {
        // Nothing to do.
    }

    @Override
    public void closeExpiredConnections() {
        // Nothing to do.
    }

    @Override
    public void shutdown() {
        // Nothing to do.
    }
}
