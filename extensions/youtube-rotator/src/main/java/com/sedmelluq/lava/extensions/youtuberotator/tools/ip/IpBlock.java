package com.sedmelluq.lava.extensions.youtuberotator.tools.ip;

import java.math.BigInteger;
import java.net.InetAddress;

public abstract class IpBlock<I extends InetAddress> {

  public abstract I getRandomAddress();

  public abstract I getAddressAtIndex(long index);

  public I getAddressAtIndex(BigInteger index) {
    return getAddressAtIndex(index.longValue());
  }

  public abstract Class<I> getType();

  public abstract BigInteger getSize();
}
