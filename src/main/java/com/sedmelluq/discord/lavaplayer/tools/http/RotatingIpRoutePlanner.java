package com.sedmelluq.discord.lavaplayer.tools.http;

import com.sedmelluq.discord.lavaplayer.tools.IpAddressTools;
import com.sedmelluq.discord.lavaplayer.tools.IpBlock;
import com.sedmelluq.discord.lavaplayer.tools.Tuple;
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

import javax.annotation.Nullable;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.function.Predicate;

public final class RotatingIpRoutePlanner implements HttpRoutePlanner {

  private static RotatingIpRoutePlanner instance = null;
  private static final Logger log = LoggerFactory.getLogger(RotatingIpRoutePlanner.class);
  private final IpBlock ipBlock;
  private final Predicate<InetAddress> ipFilter;
  private final SchemePortResolver schemePortResolver;
  private InetAddress currentAddress;
  private boolean next;
  private int index = 0;

  @Nullable
  public static RotatingIpRoutePlanner getInstance() {
    return instance;
  }

  /**
   * @param ipBlock the block to perform balancing over.
   */
  public RotatingIpRoutePlanner(final IpBlock ipBlock) {
    this(ipBlock, i -> true, DefaultSchemePortResolver.INSTANCE);
  }

  /**
   * @param ipBlock  the block to perform balancing over.
   * @param ipFilter function to filter out certain IP addresses picked from the IP block, causing another random to be chosen.
   */
  public RotatingIpRoutePlanner(final IpBlock ipBlock, final Predicate<InetAddress> ipFilter) {
    this(ipBlock, ipFilter, DefaultSchemePortResolver.INSTANCE);
  }

  /**
   * @param ipBlock            the block to perform balancing over.
   * @param ipFilter           function to filter out certain IP addresses picked from the IP block, causing another random to be chosen.
   * @param schemePortResolver for resolving ports for schemes where the port is not explicitly stated.
   */
  public RotatingIpRoutePlanner(final IpBlock ipBlock, final Predicate<InetAddress> ipFilter, final SchemePortResolver schemePortResolver) {
    this.ipBlock = ipBlock;
    this.ipFilter = ipFilter;
    this.schemePortResolver = schemePortResolver;
    instance = this;
  }

  public void next() {
    this.next = true;
  }

  @Override
  public HttpRoute determineRoute(final HttpHost host, final HttpRequest request, final HttpContext context) throws HttpException {
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
      } catch (final UnsupportedSchemeException e) {
        throw new HttpException(e.getMessage());
      }
    } else
      remotePort = host.getPort();

    final Tuple<Inet4Address, Inet6Address> remoteAddresses = IpAddressTools.getRandomAddressesFromHost(host);

    InetAddress remoteAddress;

    if (ipBlock.getType() == Inet4Address.class) {
      if (remoteAddresses.l != null) {
        if (currentAddress == null || next)
          currentAddress = extractLocalAddress();
        remoteAddress = remoteAddresses.l;
        log.info("Selected " + currentAddress.toString() + " as new outgoing ip");
      } else {
        throw new HttpException("Could not resolve " + host.getHostName());
      }
    } else if (ipBlock.getType() == Inet6Address.class) {
      if (remoteAddresses.r != null) {
        if (currentAddress == null || next)
          currentAddress = extractLocalAddress();
        remoteAddress = remoteAddresses.r;
        log.info("Selected " + currentAddress.toString() + " as new outgoing ip");
      } else if (remoteAddresses.l != null) {
        currentAddress = null;
        remoteAddress = remoteAddresses.l;
        log.warn("Could not look up AAAA record for {}. Falling back to unbalanced IPv4.", host.getHostName());
      } else {
        throw new HttpException("Could not resolve " + host.getHostName());
      }
    } else {
      throw new HttpException("Unknown IpBlock type: " + ipBlock.getType().getCanonicalName());
    }

    this.next = false;
    log.info("Calculated new route for RotateOnBan strategy: SrcIp: {}, DstIp: {}, DstPort: {}", currentAddress, remoteAddress, remotePort);
    final HttpHost target = new HttpHost(remoteAddress, host.getHostName(), remotePort, host.getSchemeName());
    final HttpHost proxy = config.getProxy();
    final boolean secure = target.getSchemeName().equalsIgnoreCase("https");
    if (proxy == null) {
      return new HttpRoute(target, currentAddress, secure);
    } else {
      return new HttpRoute(target, currentAddress, proxy, secure);
    }
  }

  private InetAddress extractLocalAddress() {
    InetAddress localAddress;
    do {
      try {
        localAddress = ipBlock.getAddressAtIndex(index++);
      } catch (final IllegalArgumentException ex) {
        log.warn("Reached end of CIDR block, starting from start again");
        index = 0;
        localAddress = null;
      }
    } while (localAddress == null || !ipFilter.test(localAddress));
    return localAddress;
  }
}
