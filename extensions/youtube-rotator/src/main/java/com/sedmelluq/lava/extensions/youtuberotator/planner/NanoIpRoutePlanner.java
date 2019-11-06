package com.sedmelluq.lava.extensions.youtuberotator.planner;

import com.sedmelluq.lava.extensions.youtuberotator.tools.BigRandom;
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
import java.util.concurrent.atomic.AtomicReference;

public final class NanoIpRoutePlanner extends AbstractRoutePlanner {

  private static final Logger log = LoggerFactory.getLogger(NanoIpRoutePlanner.class);
  private static final BigRandom random = new BigRandom();

  private final AtomicReference<BigInteger> startTime;

  public NanoIpRoutePlanner(final List<IpBlock> ipBlocks, final boolean handleSearchFailure) {
    super(ipBlocks, handleSearchFailure);
    for (IpBlock ipBlock : ipBlocks) {
      if (ipBlock.getSize().compareTo(Ipv6Block.BLOCK64_IPS) < 0)
        throw new IllegalArgumentException("Nano IP Route planner requires an IPv6Block which is greater or equal to a /64");
    }
    startTime = new AtomicReference<>(BigInteger.valueOf(System.nanoTime()));
  }

  /**
   * Returns the address offset based on the current nano time
   *
   * @return address offset as long
   */
  public long getCurrentAddress() {
    return System.nanoTime() - startTime.get().longValue();
  }

  @Override
  protected Tuple<InetAddress, InetAddress> determineAddressPair(final Tuple<Inet4Address, Inet6Address> remoteAddresses) throws HttpException {
    InetAddress currentAddress = null;
    InetAddress remoteAddress;
    final IpBlock ipBlock = getCurrentIpBlock();
    if (ipBlock.getType() == Inet4Address.class) {
      if (remoteAddresses.l != null) {
        currentAddress = getAddress(ipBlock);
        log.debug("Selected " + currentAddress.toString() + " as new outgoing ip");
        remoteAddress = remoteAddresses.l;
      } else {
        throw new HttpException("Could not resolve host");
      }
    } else if (ipBlock.getType() == Inet6Address.class) {
      if (remoteAddresses.r != null) {
        currentAddress = getAddress(ipBlock);
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
    return new Tuple<>(currentAddress, remoteAddress);
  }

  @Override
  protected void onBlockSwitch(IpBlock newBlock) {
    this.startTime.set(BigInteger.valueOf(System.nanoTime()));
  }

  private InetAddress getAddress(final IpBlock ipBlock) {
    final BigInteger now = BigInteger.valueOf(System.nanoTime());
    final BigInteger nanoOffset = now.subtract(startTime.get()); // least 64 bit
    final int maskBits = ((Ipv6Block) ipBlock).getMaskBits();
    if (maskBits == 64) {
      return ipBlock.getAddressAtIndex(nanoOffset);
    }
    final BigInteger randomOffset = random.nextBigInt(Ipv6Block.IPV6_BIT_SIZE - maskBits).shiftLeft(Ipv6Block.IPV6_BIT_SIZE - maskBits); // most {{maskBits}}-64 bit
    return ipBlock.getAddressAtIndex(randomOffset.add(nanoOffset));
  }

}
