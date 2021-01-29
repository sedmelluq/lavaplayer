package com.sedmelluq.discord.lavaplayer.tools.http;

import com.sedmelluq.discord.lavaplayer.tools.exception.DetailMessageBuilder;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.Inet6Address;
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
  private static final String RESOLVED_ADDRESSES = "lp.resolved-addresses";

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

  public static void setResolvedAddresses(HttpContext context, HttpHost host, InetAddress[] addresses) {
    if (host == null || addresses == null) {
      context.removeAttribute(RESOLVED_ADDRESSES);
    } else {
      context.setAttribute(RESOLVED_ADDRESSES, new ResolvedAddresses(host, addresses));
    }
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

    InetAddress[] addresses = resolveAddresses(host, context);
    int lastMatchIndex = lastMatchIndex(localAddress, addresses);

    for (int i = 0; i < addresses.length; i++) {
      if (!addressTypesMatch(localAddress, addresses[i])) {
        continue;
      }

      InetSocketAddress remoteAddress = new InetSocketAddress(addresses[i], port);
      boolean isLast = i == lastMatchIndex;

      try {
        boolean connected = connectWithDestination(
            socketFactory, context, socketConfig, host, localAddress, connectTimeout, connection,
            remoteAddress, addresses, isLast
        );

        if (connected) {
          return;
        }
      } catch (IOException | RuntimeException | Error e) {
        complementException(e, host, localAddress, remoteAddress, connectTimeout, addresses, i);
        throw e;
      } catch (Throwable e) {
        RuntimeException delegated = new RuntimeException(e);
        complementException(delegated, host, localAddress, remoteAddress, connectTimeout, addresses, i);
        throw delegated;
      }
    }

    NoRouteToHostException exception =
        new NoRouteToHostException("Local address protocol does not match any remote addresses.");
    complementException(exception, host, localAddress, null, connectTimeout, addresses, 0);
    throw exception;
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

  private InetAddress[] resolveAddresses(HttpHost host, HttpContext context) throws IOException {
    if (host.getAddress() != null) {
      return new InetAddress[] { host.getAddress() };
    }

    Object resolvedObject = context.getAttribute(RESOLVED_ADDRESSES);

    if (resolvedObject instanceof ResolvedAddresses) {
      ResolvedAddresses resolved = (ResolvedAddresses) resolvedObject;

      if (resolved.host.equals(host)) {
        return resolved.addresses;
      }
    }

    return dnsResolver.resolve(host.getHostName());
  }

  private boolean connectWithDestination(
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
      return true;
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

    return false;
  }

  private int lastMatchIndex(InetSocketAddress localSocketAddress, InetAddress[] remoteAddresses) {
    for (int i = remoteAddresses.length - 1; i >= 0; i--) {
      if (addressTypesMatch(localSocketAddress, remoteAddresses[i])) {
        return i;
      }
    }

    return -1;
  }

  private boolean addressTypesMatch(InetSocketAddress localSocketAddress, InetAddress remoteAddress) {
    InetAddress localAddress = localSocketAddress != null ? localSocketAddress.getAddress() : null;

    if (localAddress == null || remoteAddress == null) {
      return true;
    }

    return (localAddress instanceof Inet4Address && remoteAddress instanceof Inet4Address) ||
        (localAddress instanceof Inet6Address && remoteAddress instanceof Inet6Address);
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
    DetailMessageBuilder builder = new DetailMessageBuilder();
    builder.appendHeader("Encountered when opening a connection with the following details:");

    builder.appendField("host", host);
    builder.appendField("localAddress", localAddress);
    builder.appendField("remoteAddress", remoteAddress);
    builder.appendField("connectTimeout", connectTimeout);

    builder.appendArray("triedAddresses", false, addresses, index ->
        index <= currentIndex && addressTypesMatch(localAddress, addresses[index])
    );

    builder.appendArray("untriedAddresses", false, addresses, index ->
        index > currentIndex && addressTypesMatch(localAddress, addresses[index])
    );

    builder.appendArray("unsuitableAddresses", false, addresses, index ->
        !addressTypesMatch(localAddress, addresses[index])
    );

    exception.addSuppressed(new AdditionalDetails(builder.toString()));
  }

  private static class AdditionalDetails extends Exception {
    protected AdditionalDetails(String message) {
      super(message, null, true, false);
    }
  }

  private static class ResolvedAddresses {
    private final HttpHost host;
    private final InetAddress[] addresses;

    private ResolvedAddresses(HttpHost host, InetAddress[] addresses) {
      this.host = host;
      this.addresses = addresses;
    }
  }
}
