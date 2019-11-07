package com.sedmelluq.lava.extensions.youtuberotator.planner;

import com.sedmelluq.lava.extensions.youtuberotator.tools.Tuple;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.CombinedIpBlock;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.IpAddressTools;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.IpBlock;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class AbstractRoutePlanner implements HttpRoutePlanner {
  private static final String CHOSEN_IP_ATTRIBUTE = "yt-route-ip";

  private static final long FAILING_TIME = TimeUnit.DAYS.toMillis(7);
  private static final Logger log = LoggerFactory.getLogger(AbstractRoutePlanner.class);

  protected final IpBlock ipBlock;
  protected final Map<String, Long> failingAddresses;
  private final SchemePortResolver schemePortResolver;
  private final boolean handleSearchFailure;

  protected AbstractRoutePlanner(final List<IpBlock> ipBlocks, final boolean handleSearchFailure) {
    this.ipBlock = new CombinedIpBlock(ipBlocks);
    this.failingAddresses = new HashMap<>();
    this.schemePortResolver = DefaultSchemePortResolver.INSTANCE;
    this.handleSearchFailure = handleSearchFailure;
    log.info("Active RoutePlanner: {} using total of {} ips", getClass().getCanonicalName(), this.ipBlock.getSize());
  }

  public IpBlock getIpBlock() {
    return ipBlock;
  }

  public boolean shouldHandleSearchFailure() {
    return this.handleSearchFailure;
  }

  public Map<String, Long> getFailingAddresses() {
    return failingAddresses;
  }

  public final InetAddress getLastAddress(final HttpClientContext context) {
    return context.getAttribute(CHOSEN_IP_ATTRIBUTE, InetAddress.class);
  }

  public final void markAddressFailing(HttpClientContext context) {
    final InetAddress address = getLastAddress(context);
    if (address == null) {
      log.warn("Call to markAddressFailing() without chosen IP set",
          new RuntimeException("Report this to the devs: address is null"));
      return;
    }
    this.failingAddresses.put(address.toString(), System.currentTimeMillis());
    onAddressFailure(address);
  }

  public final void freeAddress(final InetAddress address) {
    this.failingAddresses.remove(address.toString());
  }

  public final void freeAllAddresses() {
    this.failingAddresses.clear();
  }

  protected final boolean isValidAddress(final InetAddress address) {
    final Long failedTimestamp = failingAddresses.get(address.toString());
    if (failedTimestamp == null) {
      log.debug("No failing entry for {}", address);
      return true;
    }
    if (failedTimestamp + getFailingIpsCacheDuration() < System.currentTimeMillis()) {
      failingAddresses.remove(address.toString());
      log.debug("Removing expired failing entry for {}", address);
      return true;
    }
    log.warn("{} was chosen, but is marked as failing, retrying...", address);
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
    clientContext.setAttribute(CHOSEN_IP_ATTRIBUTE, addresses.l);
    log.debug("Setting route context attribute to {}", addresses.l);
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
   * How long a failing address should not be reused in milliseconds
   *
   * @return duration in milliseconds
   */
  protected long getFailingIpsCacheDuration() {
    return FAILING_TIME;
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
