package com.sedmelluq.discord.lavaplayer.tools;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Fre_d
 */
public class Ipv6Subnet {

  private final Pattern cidrRegex = Pattern.compile("([\\da-f:]+)/(\\d{1,3})");
  private final String cidr;
  private final int maskBits; // 1-128
  private final long prefix;

  public Ipv6Subnet(String cidr) {
    this.cidr = cidr.toLowerCase();
    Matcher matcher = cidrRegex.matcher(this.cidr);
    if (!matcher.find()) {
      throw new IllegalArgumentException(cidr + " does not appear to be a valid CIDR.");
    }

    try {
      InetAddress byName = InetAddress.getByName(matcher.group(1));
      prefix = addressToLong((Inet6Address) byName);
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("Invalid IPv6 address", e);
    }
    maskBits = Integer.parseInt(matcher.group(2));

    if (maskBits > 64 || maskBits < 1) {
      throw new IllegalArgumentException("This class only handles with /1-64 subnets. Got /" + maskBits);
    }
  }

  private static InetAddress longToAddress(long l) {
    byte[] b = new byte[] {
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
      throw new RuntimeException(e);
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
