package com.sedmelluq.discord.lavaplayer.tools;

import java.net.InetAddress;

public abstract class IpBlock<I extends InetAddress> {

  public abstract I getRandomAddress();

  public abstract I getAddressAtIndex(int index);

  public abstract Class<I> getType();
}
