package com.sedmelluq.discord.lavaplayer.tools;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Fre_d
 */
public class Ipv6Subnet {

  private static final int IPV6_BIT_SIZE = 128;
  private static final int TRUNCATED_BITS = 64;
  private static final Random random = new Random();

  private static final Pattern CIDR_REGEX = Pattern.compile("([\\da-f:]+)/(\\d{1,3})");
  private final String cidr;
  private final int maskBits; // 1-128
  private final long prefix;

  public Ipv6Subnet(String cidr) {
    this.cidr = cidr.toLowerCase();
    Matcher matcher = CIDR_REGEX.matcher(this.cidr);
    if (!matcher.find()) {
      throw new IllegalArgumentException(cidr + " does not appear to be a valid CIDR.");
    }

    long unboundedPrefix;
    try {
      InetAddress address = InetAddress.getByName(matcher.group(1));
      unboundedPrefix = addressToLong((Inet6Address) address);
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("Invalid IPv6 address", e);
    }
    maskBits = Integer.parseInt(matcher.group(2));

    if (maskBits > TRUNCATED_BITS || maskBits < 1) {
      throw new IllegalArgumentException("This class only handles /1-64 subnets. Got /" + maskBits);
    }

    // Truncate all bits after $maskBits$ number of bits in the prefix
    @SuppressWarnings("ShiftOutOfRange")
    long prefixMask = Long.MAX_VALUE << (IPV6_BIT_SIZE - maskBits);
    prefix = unboundedPrefix & prefixMask;
  }

  /**
   * @return a random /64 member subnet of this subnet.
   */
  public InetAddress getRandomSlash64() {
    // Create a mask of variable length to be AND'ed with a random value
    int prefixBits = IPV6_BIT_SIZE - maskBits;
    long randMask = Long.MAX_VALUE >> prefixBits;
    long maskedRandom = random.nextLong() & randMask;

    // Combine prefix and match
    return longToAddress(prefix | maskedRandom);
  }

  @Override
  public String toString() {
    return cidr;
  }

  private static InetAddress longToAddress(long l) {
    byte[] b = new byte[]{
        (byte) l,
        (byte) (l >> 8),
        (byte) (l >> 16),
        (byte) (l >> 24),
        (byte) (l >> 32),
        (byte) (l >> 40),
        (byte) (l >> 48),
        (byte) (l >> 56),
        0, 0, 0, 0,
        0, 0, 0, 0
    };

    try {
      return Inet6Address.getByAddress(b);
    } catch (UnknownHostException e) {
      throw new RuntimeException(e); // This should not happen, as we do not do a DNS lookup
    }
  }

  /**
   * The last 64 bits are truncated
   */
  private static long addressToLong(Inet6Address address) {
    byte[] b = address.getAddress();
    return ((long) b[7] << 56)
        | ((long) b[6] & 0xff) << 48
        | ((long) b[5] & 0xff) << 40
        | ((long) b[4] & 0xff) << 32
        | ((long) b[3] & 0xff) << 24
        | ((long) b[2] & 0xff) << 16
        | ((long) b[1] & 0xff) << 8
        | ((long) b[0] & 0xff);
  }

}
