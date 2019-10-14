package com.sedmelluq.discord.lavaplayer.tools.http;

import com.sedmelluq.discord.lavaplayer.tools.Ipv6Block;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.UnsupportedSchemeException;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class BalancingIpv6RoutePlanner implements HttpRoutePlanner {

  private static final Logger log = LoggerFactory.getLogger(BalancingIpv6RoutePlanner.class);
  private final Ipv6Block ipBlock;
  private final SchemePortResolver schemePortResolver;

  public BalancingIpv6RoutePlanner(Ipv6Block ipBlock) {
    this(ipBlock, DefaultSchemePortResolver.INSTANCE);
  }

  public BalancingIpv6RoutePlanner(Ipv6Block ipBlock, SchemePortResolver schemePortResolver) {
    this.ipBlock = ipBlock;
    this.schemePortResolver = schemePortResolver;
  }

  @Override
  public HttpRoute determineRoute(HttpHost host, HttpRequest request, HttpContext context) throws HttpException {
    Args.notNull(request, "Request");
    if (host == null) {
      throw new ProtocolException("Target host is not specified");
    }
    final HttpClientContext clientContext = HttpClientContext.adapt(context);
    final RequestConfig config = clientContext.getRequestConfig();
    int remotePort;
    if (host.getPort() <= 0) {
      try {
        remotePort = schemePortResolver.resolve(host);
      } catch (UnsupportedSchemeException e) {
        throw new HttpException(e.getMessage());
      }
    } else remotePort = host.getPort();

    Stream<InetAddress> ipStream;
    try {
      ipStream = Arrays.stream(InetAddress.getAllByName(host.getHostName()));
    } catch (UnknownHostException e) {
      throw new HttpException("Could not resolve " + host.getHostName(), e);
    }

    InetAddress remoteAddress;
    InetAddress localAddress;
    Optional<InetAddress> ip6 = ipStream.filter(Inet6Address.class::isInstance).findAny();
    if (ip6.isPresent()) {
      localAddress = ipBlock.getRandomSlash64();
      remoteAddress = ip6.get();
    } else {
      Optional<InetAddress> ip4 = ipStream.filter(Inet4Address.class::isInstance).findAny();
      localAddress = null;
      remoteAddress = ip4.orElseThrow(() -> new HttpException("Could not resolve " + host.getHostName()));
      log.warn("Could not look up AAAA record for {}. Falling back to unbalanced IPv4.", host.getHostName());
    }

    HttpHost target = new HttpHost(remoteAddress, host.getHostName(), remotePort, host.getSchemeName());
    HttpHost proxy = config.getProxy();
    final boolean secure = target.getSchemeName().equalsIgnoreCase("https");
    if (proxy == null) {
      return new HttpRoute(target, localAddress, secure);
    } else {
      return new HttpRoute(target, localAddress, proxy, secure);
    }
  }
}
