package com.sedmelluq.discord.lavaplayer.tools.http;

import com.sedmelluq.discord.lavaplayer.tools.IpBlock;
import com.sedmelluq.discord.lavaplayer.tools.Tuple;
import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public final class RotatingIpRoutePlanner extends AbstractRoutePlanner {

  private static final Logger log = LoggerFactory.getLogger(RotatingIpRoutePlanner.class);
  private static final Random random = new Random();
  private final Predicate<InetAddress> ipFilter;
  private final AtomicBoolean next;
  private final AtomicInteger rotateIndex;
  private volatile InetAddress currentAddress;
  private volatile InetAddress lastFailingAddress;
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
    this(ipBlock, ipFilter, true);
  }

  /**
   * @param ipBlock             the block to perform balancing over.
   * @param ipFilter            function to filter out certain IP addresses picked from the IP block, causing another random to be chosen.
   * @param handleSearchFailure whether a search 429 should trigger the ip as failing
   */
  public RotatingIpRoutePlanner(final IpBlock ipBlock, final Predicate<InetAddress> ipFilter, final boolean handleSearchFailure) {
    super(ipBlock, handleSearchFailure);
    this.ipFilter = ipFilter;
    this.next = new AtomicBoolean(false);
    this.rotateIndex = new AtomicInteger(0);
    this.lastFailingAddress = null;
  }

  public void next() {
    rotateIndex.getAndIncrement();
    if (!this.next.compareAndSet(false, true)) {
      log.warn("Call to next() even when previous next() hasn't completed yet");
    }
  }

  public InetAddress getCurrentAddress() {
    return currentAddress;
  }

  public int getIndex() {
    return index;
  }

  public int getRotateIndex() {
    return rotateIndex.get();
  }

  @Override
  protected Tuple<InetAddress, InetAddress> determineAddressPair(final Tuple<Inet4Address, Inet6Address> remoteAddresses) throws HttpException {
    InetAddress remoteAddress;
    if (ipBlock.getType() == Inet4Address.class) {
      if (remoteAddresses.l != null) {
        if (currentAddress == null || next.get()) {
          currentAddress = extractLocalAddress();
          log.info("Selected " + currentAddress.toString() + " as new outgoing ip");
        }
        remoteAddress = remoteAddresses.l;
      } else {
        throw new HttpException("Could not resolve host");
      }
    } else if (ipBlock.getType() == Inet6Address.class) {
      if (remoteAddresses.r != null) {
        if (currentAddress == null || next.get()) {
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
    next.set(false);
    return new Tuple<>(currentAddress, remoteAddress);
  }

  @Override
  protected void onAddressFailure(final InetAddress address) {
    if (lastFailingAddress != null && lastFailingAddress.toString().equals(address.toString())) {
      log.warn("Address {} was already failing, not triggering next()", address.toString());
      return;
    }
    lastFailingAddress = address;
    next();
  }

  private InetAddress extractLocalAddress() {
    InetAddress localAddress;
    int it = 0;
    do {
      if (it++ > ipBlock.getSize() * 1.5) {
        throw new RuntimeException("Can't find a free ip");
      }
      if (ipBlock.getSize() >= 128)
        index += random.nextInt(10) + 1;
      else
        ++index;
      try {
        localAddress = ipBlock.getAddressAtIndex(index - 1);
      } catch (final Exception ex) {
        index = 0;
        localAddress = null;
      }
    } while (localAddress == null || !ipFilter.test(localAddress) || !isValidAddress(localAddress));
    return localAddress;
  }
}
