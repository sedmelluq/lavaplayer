package com.sedmelluq.lava.extensions.youtuberotator.tools.ip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Ipv4Block extends IpBlock<Inet4Address> {

  public static boolean isIpv4CidrBlock(String cidr) {
    if (!cidr.contains("/"))
      cidr += "/32";
    return CIDR_REGEX.matcher(cidr).matches();
  }

  private static final Pattern CIDR_REGEX = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})/(\\d{1,2})");
  private static final int NBITS = 32;
  private static final Logger log = LoggerFactory.getLogger(Ipv4Block.class);
  private static final Random random = new Random();

  private static int matchAddress(Matcher matcher) {
    int addr = 0;
    for (int i = 1; i <= 4; ++i) {
      int n = (rangeCheck(Integer.parseInt(matcher.group(i)), 0, 255));
      addr |= ((n & 0xff) << 8 * (4 - i));
    }
    return addr;
  }

  private static int rangeCheck(int value, int begin, int end) {
    if (value >= begin && value <= end) { // (begin,end]
      return value;
    }
    throw new IllegalArgumentException("Value [" + value + "] not in range [" + begin + "," + end + "]");
  }

  private final int maskBits;
  private final int address;

  public Ipv4Block(String cidr) {
    if (!cidr.contains("/"))
      cidr += "/32";
    final Matcher matcher = CIDR_REGEX.matcher(cidr);
    if (matcher.matches()) {
      this.address = matchAddress(matcher);
      this.maskBits = Integer.parseInt(matcher.group(5));
    } else
      throw new IllegalArgumentException("Could not parse [" + cidr + "]");
    log.info("Using Ipv4Block with {} addresses", getSize());
  }

  @Override
  public Inet4Address getRandomAddress() {
    if (maskBits == NBITS) return intToAddress(address);

    final int randMask = Integer.MAX_VALUE >> maskBits - 1;
    final int maskedRandom = random.nextInt() & randMask;

    final Inet4Address inetAddress = intToAddress(address + maskedRandom);
    log.info(inetAddress.toString());
    return inetAddress;
  }

  @Override
  public Inet4Address getAddressAtIndex(final long index) {
    if (index > Math.pow(2, NBITS - maskBits))
      throw new IllegalArgumentException("Index out of bounds for provided CIDR Block");
    return intToAddress(address + (int) index);
  }

  @Override
  public Class<Inet4Address> getType() {
    return Inet4Address.class;
  }

  @Override
  public BigInteger getSize() {
    return BigInteger.valueOf(2).pow(NBITS - maskBits);
  }

  @Override
  public int getMaskBits() {
    return maskBits;
  }

  private Inet4Address intToAddress(final int val) {
    byte[] octets = new byte[4];
    for (int j = 3; j >= 0; --j) {
      octets[j] |= ((val >>> 8 * (3 - j)) & (0xff));
    }
    try {
      return (Inet4Address) Inet4Address.getByAddress(octets);
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

}
