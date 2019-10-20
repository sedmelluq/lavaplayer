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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class AbstractRoutePlanner implements HttpRoutePlanner {

  private static final long FAILING_TIME = TimeUnit.HOURS.toMillis(1);
  private static final Logger log = LoggerFactory.getLogger(AbstractRoutePlanner.class);

  private static AbstractRoutePlanner activePlanner;

  @Nullable
  public static AbstractRoutePlanner getActivePlanner() {
    return activePlanner;
  }

  protected final IpBlock ipBlock;
  protected final Map<String, Long> failingAddresses;
  private final SchemePortResolver schemePortResolver;
  private InetAddress lastAddress;

  protected AbstractRoutePlanner(final IpBlock ipBlock) {
    this.ipBlock = ipBlock;
    this.failingAddresses = new HashMap<>();
    this.schemePortResolver = DefaultSchemePortResolver.INSTANCE;
    activePlanner = this;
    log.info("Active RoutePlanner: {}", getClass().getCanonicalName());
  }

  public final InetAddress getLastAddress() {
    return this.lastAddress;
  }

  public final void markAddressFailing() {
    this.failingAddresses.put(this.lastAddress.toString(), System.currentTimeMillis());
    onAddressFailure(this.lastAddress);
  }

  public void freeAddress(final InetAddress address) {
    this.failingAddresses.remove(address.toString());
  }

  public void freeAllAddresses() {
    this.failingAddresses.clear();
  }

  protected final boolean isValidAddress(final InetAddress address) {
    final Long failedTimestamp = failingAddresses.get(address.toString());
    if (failedTimestamp == null) {
      log.debug("No failing entry for {}", address);
      return true;
    }
    if (failedTimestamp + FAILING_TIME < System.currentTimeMillis()) {
      failingAddresses.remove(address.toString());
      log.debug("Removing expired failing entry for {}", address);
      return true;
    }
    log.info("{} was chosen, but is marked as failing");
    return false;
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
    final Tuple<InetAddress, InetAddress> addresses = determineAddressPair(remoteAddresses);

    final HttpHost target = new HttpHost(addresses.r, host.getHostName(), remotePort, host.getSchemeName());
    final HttpHost proxy = config.getProxy();
    final boolean secure = target.getSchemeName().equalsIgnoreCase("https");
    this.lastAddress = addresses.l;
    log.info("Setting last address to {}", lastAddress);
    if (proxy == null) {
      return new HttpRoute(target, addresses.l, secure);
    } else {
      return new HttpRoute(target, addresses.l, proxy, secure);
    }
  }

  /**
   * Called when an address is marked as failing
   *
   * @param address the failing address
   */
  protected void onAddressFailure(final InetAddress address) {

  }

  /**
   * Determines the local and remote address pair to use
   *
   * @param remoteAddresses The remote address pair containing IPv4 and IPv6 addresses - which can be null
   * @return a {@link Tuple} which contains l = localAddress & r = remoteAddress
   * @throws HttpException when no route can be determined
   */
  protected abstract Tuple<InetAddress, InetAddress> determineAddressPair(final Tuple<Inet4Address, Inet6Address> remoteAddresses) throws HttpException;
}
