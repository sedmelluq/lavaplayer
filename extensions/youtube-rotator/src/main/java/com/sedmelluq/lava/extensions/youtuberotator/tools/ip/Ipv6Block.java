package com.sedmelluq.lava.extensions.youtuberotator.tools.ip;

import com.sedmelluq.lava.extensions.youtuberotator.tools.BigRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Frederik Arbjerg Mikkelsen
 */
public class Ipv6Block extends IpBlock<Inet6Address> {

  public static boolean isIpv6CidrBlock(String cidr) {
    if (!cidr.contains("/"))
      cidr += "/128";
    return CIDR_REGEX.matcher(cidr).matches();
  }

  private static final BigInteger TWO = BigInteger.valueOf(2);
  private static final BigInteger BITS1 = BigInteger.valueOf(-1);
  public static final BigInteger BLOCK64_IPS = TWO.pow(64);
  public static final int IPV6_BIT_SIZE = 128;

  private static final BigRandom random = new BigRandom();

  private static final Pattern CIDR_REGEX = Pattern.compile("([\\da-f:]+)/(\\d{1,3})");
  private final String cidr;
  private final int maskBits;
  private final BigInteger prefix;
  private static final Logger log = LoggerFactory.getLogger(Ipv6Block.class);

  public Ipv6Block(String cidr) {
    if (!cidr.contains("/"))
      cidr += "/128";
    this.cidr = cidr.toLowerCase();
    Matcher matcher = CIDR_REGEX.matcher(this.cidr);
    if (!matcher.find()) {
      throw new IllegalArgumentException(cidr + " does not appear to be a valid CIDR.");
    }

    BigInteger unboundedPrefix;
    try {
      InetAddress address = InetAddress.getByName(matcher.group(1));
      unboundedPrefix = addressToLong((Inet6Address) address);
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("Invalid IPv6 address", e);
    }
    maskBits = Integer.parseInt(matcher.group(2));

    BigInteger prefixMask = BITS1.shiftLeft(IPV6_BIT_SIZE - maskBits - 1);
    prefix = unboundedPrefix.and(prefixMask);
    log.info("Using Ipv6Block with {} addresses", getSize());
  }

  /**
   * @return a random member of this subnet.
   */
  @Override
  public Inet6Address getRandomAddress() {
    if (maskBits == IPV6_BIT_SIZE) return longToAddress(prefix);

    final BigInteger randomAddressOffset = random.nextBigInt(IPV6_BIT_SIZE - (maskBits + 1)).abs();
    Inet6Address inetAddress = longToAddress(prefix.add(randomAddressOffset));
    log.info(inetAddress.toString());
    return inetAddress;
  }

  @Override
  public Inet6Address getAddressAtIndex(long index) {
    return getAddressAtIndex(BigInteger.valueOf(index));
  }

  @Override
  public Inet6Address getAddressAtIndex(final BigInteger index) {
    if (index.compareTo(getSize()) > 0)
      throw new IllegalArgumentException("Index out of bounds for provided CIDR Block");
    return longToAddress(prefix.add(index));
  }

  @Override
  public Class<Inet6Address> getType() {
    return Inet6Address.class;
  }

  @Override
  public BigInteger getSize() {
    return TWO.pow(IPV6_BIT_SIZE - maskBits);
  }

  @Override
  public String toString() {
    return cidr;
  }

  public int getMaskBits() {
    return maskBits;
  }

  private static Inet6Address longToAddress(final BigInteger l) {
    final byte[] b = new byte[IPV6_BIT_SIZE / 8];
    final int start = (b.length - 1) * 8;
    for (int i = 0; i < b.length; i++) {
      int shift = start - i * 8;
      if (shift > 0)
        b[i] = l.shiftRight(start - i * 8).byteValue();
      else
        b[i] = l.byteValue();
    }
    try {
      return (Inet6Address) Inet6Address.getByAddress(b);
    } catch (final UnknownHostException e) {
      throw new RuntimeException(e); // This should not happen, as we do not do a DNS lookup
    }
  }

  private static BigInteger addressToLong(final Inet6Address address) {
    return bytesToLong(address.getAddress());
  }

  private static BigInteger bytesToLong(final byte[] b) {
    BigInteger value = BigInteger.valueOf(0);
    final int start = (b.length - 1) * 8;
    value = value.or(BigInteger.valueOf(b[0]).shiftLeft(start));
    for (int i = 1; i < b.length; i++) {
      final int shift = start - i * 8;
      if (shift > 0)
        value = value.or(BigInteger.valueOf(b[i] & 0xff).shiftLeft(shift));
      else
        value = value.or(BigInteger.valueOf(b[i] & 0xff));
    }
    return value;
  }

}