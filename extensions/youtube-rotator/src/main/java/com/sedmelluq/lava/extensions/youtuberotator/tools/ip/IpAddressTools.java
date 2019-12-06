package com.sedmelluq.lava.extensions.youtuberotator.tools.ip;

import com.sedmelluq.lava.extensions.youtuberotator.tools.Tuple;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public final class IpAddressTools {

  private static final Random RANDOM = new Random();

  public static Tuple<Inet4Address, Inet6Address> getRandomAddressesFromHost(final HttpHost host) throws HttpException {
    final List<InetAddress> ipList;
    try {
      ipList = Arrays.asList(InetAddress.getAllByName(host.getHostName()));
    } catch (final UnknownHostException e) {
      throw new HttpException("Could not resolve " + host.getHostName(), e);
    }
    final List<Inet6Address> ip6 = new ArrayList<>();
    final List<Inet4Address> ip4 = new ArrayList<>();

    Collections.reverse(ipList);
    for (final InetAddress ip : ipList) {
      if (ip instanceof Inet6Address)
        ip6.add((Inet6Address) ip);
      else if (ip instanceof Inet4Address)
        ip4.add((Inet4Address) ip);
    }
    return new Tuple<>(getRandomFromList(ip4), getRandomFromList(ip6));
  }

  public static <T> T getRandomFromList(final List<T> list) {
    if (list.isEmpty())
      return null;
    if (list.size() == 1)
      return list.get(0);
    return list.get(RANDOM.nextInt(list.size()));
  }
}
