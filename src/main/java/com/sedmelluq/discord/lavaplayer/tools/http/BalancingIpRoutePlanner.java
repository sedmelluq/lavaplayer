package com.sedmelluq.discord.lavaplayer.tools.http;

import com.sedmelluq.discord.lavaplayer.tools.IpBlock;
import com.sedmelluq.discord.lavaplayer.tools.Tuple;
import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.function.Predicate;

/**
 * @author Frederik Arbjerg Mikkelsen
 */
@SuppressWarnings("WeakerAccess")
public class BalancingIpRoutePlanner extends AbstractRoutePlanner {

  private static final Logger log = LoggerFactory.getLogger(BalancingIpRoutePlanner.class);
  private final Predicate<InetAddress> ipFilter;

  /**
   * @param ipBlock the block to perform balancing over.
   */
  public BalancingIpRoutePlanner(IpBlock ipBlock) {
    this(ipBlock, i -> true);
  }

  /**
   * @param ipBlock  the block to perform balancing over.
   * @param ipFilter function to filter out certain IP addresses picked from the IP block, causing another random to be chosen.
   */
  public BalancingIpRoutePlanner(IpBlock ipBlock, Predicate<InetAddress> ipFilter) {
    this(ipBlock, ipFilter, true);
  }

  /**
   * @param ipBlock             the block to perform balancing over.
   * @param ipFilter            function to filter out certain IP addresses picked from the IP block, causing another random to be chosen.
   * @param handleSearchFailure whether a search 429 should trigger the ip as failing
   */
  public BalancingIpRoutePlanner(IpBlock ipBlock, Predicate<InetAddress> ipFilter, boolean handleSearchFailure) {
    super(ipBlock, handleSearchFailure);
    this.ipFilter = ipFilter;
  }

  @Override
  protected Tuple<InetAddress, InetAddress> determineAddressPair(Tuple<Inet4Address, Inet6Address> remoteAddresses) throws HttpException {
    InetAddress localAddress;
    final InetAddress remoteAddress;
    if (ipBlock.getType() == Inet4Address.class) {
      if (remoteAddresses.l != null) {
        localAddress = getRandomAddress(ipBlock);
        remoteAddress = remoteAddresses.l;
      } else {
        throw new HttpException("Could not resolve host");
      }
    } else if (ipBlock.getType() == Inet6Address.class) {
      if (remoteAddresses.r != null) {
        localAddress = getRandomAddress(ipBlock);
        remoteAddress = remoteAddresses.r;
      } else if (remoteAddresses.l != null) {
        localAddress = null;
        remoteAddress = remoteAddresses.l;
        log.warn("Could not look up AAAA record for host. Falling back to unbalanced IPv4.");
      } else {
        throw new HttpException("Could not resolve host");
      }
    } else {
      throw new HttpException("Unknown IpBlock type: " + ipBlock.getType().getCanonicalName());
    }
    return new Tuple<>(localAddress, remoteAddress);
  }

  private InetAddress getRandomAddress(final IpBlock ipBlock) {
    InetAddress localAddress;
    int it = 0;
    do {
      if (it++ > ipBlock.getSize() * 2) {
        throw new RuntimeException("Can't find a free ip");
      }
      localAddress = ipBlock.getRandomAddress();
    } while (localAddress == null || !ipFilter.test(localAddress) || !isValidAddress(localAddress));
    return localAddress;
  }
}
