package com.sedmelluq.discord.lavaplayer.tools.http;

import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sun.org.apache.xerces.internal.impl.XMLEntityScanner;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import org.apache.http.HttpHost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Lookup;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpClientConnectionOperator;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.UnsupportedSchemeException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.protocol.HttpContext;

public class ExtendedConnectionOperator implements HttpClientConnectionOperator {
  private static final String SOCKET_FACTORY_REGISTRY = "http.socket-factory-registry";

  private final Lookup<ConnectionSocketFactory> socketFactoryRegistry;
  private final SchemePortResolver schemePortResolver;
  private final DnsResolver dnsResolver;

  public ExtendedConnectionOperator(
      Lookup<ConnectionSocketFactory> socketFactoryRegistry,
      SchemePortResolver schemePortResolver,
      DnsResolver dnsResolver
  ) {
    this.socketFactoryRegistry = socketFactoryRegistry;
    this.schemePortResolver = schemePortResolver != null ? schemePortResolver : DefaultSchemePortResolver.INSTANCE;
    this.dnsResolver = dnsResolver != null ? dnsResolver : SystemDefaultDnsResolver.INSTANCE;
  }

  @Override
  public void connect(
      ManagedHttpClientConnection connection,
      HttpHost host,
      InetSocketAddress localAddress,
      int connectTimeout,
      SocketConfig socketConfig,
      HttpContext context
  ) throws IOException {
    ConnectionSocketFactory socketFactory = getSocketFactory(host, context);

    int port = schemePortResolver.resolve(host);
    InetAddress[] addresses = host.getAddress() != null ?
        new InetAddress[] { host.getAddress() } : this.dnsResolver.resolve(host.getHostName());

    for (int i = 0; i < addresses.length; i++) {
      InetSocketAddress remoteAddress = new InetSocketAddress(addresses[i], port);

      try {
        connectWithDestination(socketFactory, context, socketConfig, host, localAddress, connectTimeout, connection,
            remoteAddress, addresses, i == addresses.length - 1);
      } catch (IOException | RuntimeException | Error e) {
        complementException(e, host, localAddress, remoteAddress, connectTimeout, addresses, i);
        throw e;
      } catch (Throwable e) {
        RuntimeException delegated = new RuntimeException(e);
        complementException(delegated, host, localAddress, remoteAddress, connectTimeout, addresses, i);
        throw delegated;
      }
    }
  }

  @Override
  public void upgrade(ManagedHttpClientConnection connection, HttpHost host, HttpContext context) throws IOException {
    ConnectionSocketFactory socketFactory = getSocketFactory(host, HttpClientContext.adapt(context));

    if (!(socketFactory instanceof LayeredConnectionSocketFactory)) {
      throw new UnsupportedSchemeException(host.getSchemeName() +
          " protocol does not support connection upgrade");
    }

    LayeredConnectionSocketFactory layeredFactory = (LayeredConnectionSocketFactory) socketFactory;

    Socket socket = connection.getSocket();
    int port = this.schemePortResolver.resolve(host);
    socket = layeredFactory.createLayeredSocket(socket, host.getHostName(), port, context);

    connection.bind(socket);
  }

  private void connectWithDestination(
      ConnectionSocketFactory socketFactory,
      HttpContext context,
      SocketConfig socketConfig,
      HttpHost host,
      InetSocketAddress localAddress,
      int connectTimeout,
      ManagedHttpClientConnection connection,
      InetSocketAddress remoteAddress,
      InetAddress[] addresses,
      boolean last
  ) throws IOException {
    Socket socket = socketFactory.createSocket(context);
    configureSocket(socket, socketConfig);

    try {
      socket = socketFactory.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);
      connection.bind(socket);
    } catch (final SocketTimeoutException ex) {
      if (last) {
        throw new ConnectTimeoutException(ex, host, addresses);
      }
    } catch (final ConnectException ex) {
      if (last) {
        final String msg = ex.getMessage();
        throw "Connection timed out".equals(msg)
            ? new ConnectTimeoutException(ex, host, addresses)
            : new HttpHostConnectException(ex, host, addresses);
      }
    } catch (final NoRouteToHostException ex) {
      if (last) {
        throw ex;
      }
    }
  }

  private void configureSocket(Socket socket, SocketConfig socketConfig) throws IOException {
    socket.setSoTimeout(socketConfig.getSoTimeout());
    socket.setReuseAddress(socketConfig.isSoReuseAddress());
    socket.setTcpNoDelay(socketConfig.isTcpNoDelay());
    socket.setKeepAlive(socketConfig.isSoKeepAlive());

    if (socketConfig.getRcvBufSize() > 0) {
      socket.setReceiveBufferSize(socketConfig.getRcvBufSize());
    }

    if (socketConfig.getSndBufSize() > 0) {
      socket.setSendBufferSize(socketConfig.getSndBufSize());
    }

    if (socketConfig.getSoLinger() >= 0) {
      socket.setSoLinger(true, socketConfig.getSoLinger());
    }
  }

  private ConnectionSocketFactory getSocketFactory(HttpHost host, HttpContext context) throws IOException {
    Lookup<ConnectionSocketFactory> registry = getSocketFactoryRegistry(context);
    ConnectionSocketFactory socketFactory = registry.lookup(host.getSchemeName());

    if (socketFactory == null) {
      throw new UnsupportedSchemeException(host.getSchemeName() + " protocol is not supported");
    }

    return socketFactory;
  }

  @SuppressWarnings("unchecked")
  private Lookup<ConnectionSocketFactory> getSocketFactoryRegistry(HttpContext context) {
    Lookup<ConnectionSocketFactory> registry = (Lookup<ConnectionSocketFactory>)
        context.getAttribute(SOCKET_FACTORY_REGISTRY);

    if (registry == null) {
      registry = this.socketFactoryRegistry;
    }

    return registry;
  }

  private void complementException(
      Throwable exception,
      HttpHost host,
      InetSocketAddress localAddress,
      InetSocketAddress remoteAddress,
      int connectTimeout,
      InetAddress[] addresses,
      int currentIndex
  ) {
    StringBuilder builder = new StringBuilder();
    builder.append("Encountered when opening a connection with the following details:");

    appendField(builder, "host", host);
    appendField(builder, "localAddress", localAddress);
    appendField(builder, "remoteAddress", remoteAddress);

    builder.append("\n  connectTimeout: ").append(connectTimeout);

    appendAddresses(builder, "triedAddresses", addresses, 0, currentIndex - 1);
    appendAddresses(builder, "untriedAddresses", addresses, currentIndex + 1, addresses.length - 1);

    exception.addSuppressed(new AdditionalDetails(builder.toString()));
  }

  private void appendField(StringBuilder builder, String name, Object field) {
    builder.append("\n  ").append(name).append(": ");

    if (field == null) {
      builder.append("<unspecified>");
    } else {
      builder.append(field.toString());
    }
  }

  private void appendAddresses(StringBuilder builder, String label, InetAddress[] array, int first, int last) {
    if (first <= last) {
      builder.append("\n  ").append(label).append(": ");

      for (int i = first; i <= last; i++) {
        builder.append(array[i]).append(", ");
      }

      builder.setLength(builder.length() - 2);
    }
  }

  private static class AdditionalDetails extends Exception {
    protected AdditionalDetails(String message) {
      super(message, null, true, false);
    }
  }
}
