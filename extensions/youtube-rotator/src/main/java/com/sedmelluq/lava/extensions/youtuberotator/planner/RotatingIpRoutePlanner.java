package com.sedmelluq.lava.extensions.youtuberotator.planner;

import com.sedmelluq.lava.extensions.youtuberotator.tools.Tuple;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.IpBlock;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block;
import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public final class RotatingIpRoutePlanner extends AbstractRoutePlanner {

  private static final Logger log = LoggerFactory.getLogger(RotatingIpRoutePlanner.class);
  private static final Random random = new Random();
  private final Predicate<InetAddress> ipFilter;
  private final AtomicBoolean next;
  private final AtomicReference<BigInteger> rotateIndex;
  private final AtomicReference<BigInteger> index;
  private volatile InetAddress lastFailingAddress;

  /**
   * @param ipBlocks the block to perform balancing over.
   */
  public RotatingIpRoutePlanner(final List<IpBlock> ipBlocks) {
    this(ipBlocks, i -> true);
  }

  /**
   * @param ipBlocks  the block to perform balancing over.
   * @param ipFilter function to filter out certain IP addresses picked from the IP block, causing another random to be chosen.
   */
  public RotatingIpRoutePlanner(final List<IpBlock> ipBlocks, final Predicate<InetAddress> ipFilter) {
    this(ipBlocks, ipFilter, true);
  }

  /**
   * @param ipBlocks             the block to perform balancing over.
   * @param ipFilter            function to filter out certain IP addresses picked from the IP block, causing another random to be chosen.
   * @param handleSearchFailure whether a search 429 should trigger the ip as failing
   */
  public RotatingIpRoutePlanner(final List<IpBlock> ipBlocks, final Predicate<InetAddress> ipFilter, final boolean handleSearchFailure) {
    super(ipBlocks, handleSearchFailure);
    this.ipFilter = ipFilter;
    this.next = new AtomicBoolean(false);
    this.rotateIndex = new AtomicReference<>(BigInteger.valueOf(0));
    this.index = new AtomicReference<>(BigInteger.valueOf(0));
    this.lastFailingAddress = null;
  }

  public void next() {
    rotateIndex.accumulateAndGet(BigInteger.ONE, BigInteger::add);
    if (!this.next.compareAndSet(false, true)) {
      log.warn("Call to next() even when previous next() hasn't completed yet");
    }
  }

  public InetAddress getCurrentAddress() {
    if (index.get().compareTo(BigInteger.ZERO) == 0)
      return null;
    return ipBlock.getAddressAtIndex(index.get().subtract(BigInteger.ONE));
  }

  public BigInteger getIndex() {
    return index.get();
  }

  public BigInteger getRotateIndex() {
    return rotateIndex.get();
  }

  @Override
  protected Tuple<InetAddress, InetAddress> determineAddressPair(final Tuple<Inet4Address, Inet6Address> remoteAddresses) throws HttpException {
    InetAddress currentAddress = null;
    InetAddress remoteAddress;
    if (ipBlock.getType() == Inet4Address.class) {
      if (remoteAddresses.l != null) {
        if (index.get().compareTo(BigInteger.ZERO) == 0 || next.get()) {
          currentAddress = extractLocalAddress();
          log.info("Selected " + currentAddress.toString() + " as new outgoing ip");
        }
        remoteAddress = remoteAddresses.l;
      } else {
        throw new HttpException("Could not resolve host");
      }
    } else if (ipBlock.getType() == Inet6Address.class) {
      if (remoteAddresses.r != null) {
        if (index.get().compareTo(BigInteger.ZERO) == 0 || next.get()) {
          currentAddress = extractLocalAddress();
          log.info("Selected " + currentAddress.toString() + " as new outgoing ip");
        }
        remoteAddress = remoteAddresses.r;
      } else if (remoteAddresses.l != null) {
        remoteAddress = remoteAddresses.l;
        log.warn("Could not look up AAAA record for host. Falling back to unbalanced IPv4.");
      } else {
        throw new HttpException("Could not resolve host");
      }
    } else {
      throw new HttpException("Unknown IpBlock type: " + ipBlock.getType().getCanonicalName());
    }

    if (currentAddress == null && index.get().compareTo(BigInteger.ZERO) > 0)
      currentAddress = ipBlock.getAddressAtIndex(index.get());
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
    long triesSinceBlockSkip = 0;
    BigInteger it = BigInteger.valueOf(0);
    do {
      if (ipBlock.getSize().multiply(BigInteger.valueOf(2)).compareTo(it) < 0) {
        throw new RuntimeException("Can't find a free ip");
      }
      if (ipBlock.getSize().compareTo(BigInteger.valueOf(128)) > 0)
        index.accumulateAndGet(BigInteger.valueOf(random.nextInt(10) + 10), BigInteger::add);
      else
        index.accumulateAndGet(BigInteger.ONE, BigInteger::add);
      it = it.add(BigInteger.ONE);
      triesSinceBlockSkip++;
      if (ipBlock.getSize().compareTo(Ipv6Block.BLOCK64_IPS) > 0 && triesSinceBlockSkip > 128) {
        triesSinceBlockSkip = 0;
        rotateIndex.accumulateAndGet(Ipv6Block.BLOCK64_IPS.add(BigInteger.valueOf(random.nextLong())), BigInteger::add);
      }
      try {
        localAddress = ipBlock.getAddressAtIndex(index.get());
      } catch (final Exception ex) {
        index.set(BigInteger.ZERO);
        localAddress = null;
      }
    } while (localAddress == null || !ipFilter.test(localAddress) || !isValidAddress(localAddress));
    return localAddress;
  }
}
