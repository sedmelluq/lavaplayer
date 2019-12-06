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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public final class RotatingNanoIpRoutePlanner extends AbstractRoutePlanner {

  private static final Logger log = LoggerFactory.getLogger(RotatingNanoIpRoutePlanner.class);

  private final Predicate<InetAddress> ipFilter;
  private final AtomicReference<BigInteger> currentBlock;
  private final AtomicReference<BigInteger> blockNanoStart;
  private final AtomicBoolean next;

  public RotatingNanoIpRoutePlanner(final List<IpBlock> ipBlocks) {
    this(ipBlocks, ip -> true);
  }

  public RotatingNanoIpRoutePlanner(final List<IpBlock> ipBlocks, final Predicate<InetAddress> ipFilter) {
    this(ipBlocks, ipFilter, true);
  }

  public RotatingNanoIpRoutePlanner(final List<IpBlock> ipBlocks, final Predicate<InetAddress> ipFilter, final boolean handleSearchFailure) {
    super(ipBlocks, handleSearchFailure);
    this.ipFilter = ipFilter;
    this.currentBlock = new AtomicReference<>(BigInteger.ZERO);
    this.blockNanoStart = new AtomicReference<>(BigInteger.valueOf(System.nanoTime()));
    this.next = new AtomicBoolean(false);
    if (ipBlock.getType() != Inet6Address.class || ipBlock.getSize().compareTo(Ipv6Block.BLOCK64_IPS) < 0)
      throw new IllegalArgumentException("Please use a bigger IPv6 Block!");
  }

  /**
   * Returns the current block index
   * @return block index which is currently used
   */
  public BigInteger getCurrentBlock() {
    return currentBlock.get();
  }

  /**
   * Returns the address offset for the current nano time
   * @return address offset as long
   */
  public long getAddressIndexInBlock() {
    return System.nanoTime() - blockNanoStart.get().longValue();
  }

  @Override
  protected Tuple<InetAddress, InetAddress> determineAddressPair(final Tuple<Inet4Address, Inet6Address> remoteAddresses) throws HttpException {
    InetAddress currentAddress = null;
    InetAddress remoteAddress;
    if (ipBlock.getType() == Inet6Address.class) {
      if (remoteAddresses.r != null) {
        currentAddress = extractLocalAddress();
        log.debug("Selected " + currentAddress.toString() + " as new outgoing ip");
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
    next.set(false);
    return new Tuple<>(currentAddress, remoteAddress);
  }

  @Override
  protected void onAddressFailure(final InetAddress address) {
    currentBlock.accumulateAndGet(BigInteger.ONE, BigInteger::add);
    blockNanoStart.set(BigInteger.valueOf(System.nanoTime()));
  }

  private InetAddress extractLocalAddress() {
    InetAddress localAddress;
    long triesSinceBlockSkip = 0;
    BigInteger it = BigInteger.valueOf(0);
    do {
      try {
        if (ipBlock.getSize().multiply(BigInteger.valueOf(2)).compareTo(it) < 0) {
          throw new RuntimeException("Can't find a free ip");
        }
        it = it.add(BigInteger.ONE);
        triesSinceBlockSkip++;
        if (triesSinceBlockSkip > 128) {
          this.currentBlock.accumulateAndGet(BigInteger.ONE, BigInteger::add);
        }
        final BigInteger nanoTime = BigInteger.valueOf(System.nanoTime());
        final BigInteger timeOffset = nanoTime.subtract(blockNanoStart.get());
        final BigInteger blockOffset = currentBlock.get().multiply(Ipv6Block.BLOCK64_IPS);
        localAddress = ipBlock.getAddressAtIndex(blockOffset.add(timeOffset));
      } catch (final IllegalArgumentException ex) {
        this.currentBlock.set(BigInteger.ZERO);
        localAddress = null;
      }
    } while (localAddress == null || !ipFilter.test(localAddress) || !isValidAddress(localAddress));
    return localAddress;
  }
}
