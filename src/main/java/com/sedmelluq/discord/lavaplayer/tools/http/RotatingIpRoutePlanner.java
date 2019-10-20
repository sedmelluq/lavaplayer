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

public final class RotatingIpRoutePlanner extends AbstractRoutePlanner {

  private static final Logger log = LoggerFactory.getLogger(RotatingIpRoutePlanner.class);
  private final Predicate<InetAddress> ipFilter;
  private InetAddress currentAddress;
  private boolean next;
  private int index = 0;

  /**
   * @param ipBlock the block to perform balancing over.
   */
  public RotatingIpRoutePlanner(final IpBlock ipBlock) {
    this(ipBlock, i -> true);
  }

  /**
   * @param ipBlock  the block to perform balancing over.
   * @param ipFilter function to filter out certain IP addresses picked from the IP block, causing another random to be chosen.
   */
  public RotatingIpRoutePlanner(final IpBlock ipBlock, final Predicate<InetAddress> ipFilter) {
    super(ipBlock);
    this.ipFilter = ipFilter;
  }

  public void next() {
    this.next = true;
  }

  @Override
  protected Tuple<InetAddress, InetAddress> determineAddressPair(final Tuple<Inet4Address, Inet6Address> remoteAddresses) throws HttpException {
    InetAddress remoteAddress;
    if (ipBlock.getType() == Inet4Address.class) {
      if (remoteAddresses.l != null) {
        if (currentAddress == null || next) {
          currentAddress = extractLocalAddress();
          log.info("Selected " + currentAddress.toString() + " as new outgoing ip");
        }
        remoteAddress = remoteAddresses.l;
      } else {
        throw new HttpException("Could not resolve host");
      }
    } else if (ipBlock.getType() == Inet6Address.class) {
      if (remoteAddresses.r != null) {
        if (currentAddress == null || next) {
          currentAddress = extractLocalAddress();
          log.info("Selected " + currentAddress.toString() + " as new outgoing ip");
        }
        remoteAddress = remoteAddresses.r;
      } else if (remoteAddresses.l != null) {
        currentAddress = null;
        remoteAddress = remoteAddresses.l;
        log.warn("Could not look up AAAA record for host. Falling back to unbalanced IPv4.");
      } else {
        throw new HttpException("Could not resolve host");
      }
    } else {
      throw new HttpException("Unknown IpBlock type: " + ipBlock.getType().getCanonicalName());
    }
    this.next = false;
    return new Tuple<>(currentAddress, remoteAddress);
  }

  @Override
  protected void onAddressFailure(final InetAddress address) {
    next();
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
    } while (localAddress == null || !ipFilter.test(localAddress) || !isValidAddress(localAddress));
    return localAddress;
  }
}
