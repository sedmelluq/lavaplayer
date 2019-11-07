package com.sedmelluq.lava.extensions.youtuberotator.tools.ip;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public final class CombinedIpBlock extends IpBlock {

  private static final Random random = new Random();

  private final Class type;
  private final List<IpBlock> ipBlocks;
  private final BigInteger size;
  private final int[] hitProbability;
  private final ReentrantLock lock;

  public CombinedIpBlock(final List<IpBlock> ipBlocks) {
    if (ipBlocks.size() == 0)
      throw new IllegalArgumentException("Ip Blocks list size must be greater than zero");
    this.type = ipBlocks.get(0).getType();
    if (ipBlocks.stream().anyMatch(block -> !block.getType().equals(type)))
      throw new IllegalArgumentException("All Ip Blocks must have the same type for a combined block");
    this.ipBlocks = ipBlocks;
    this.hitProbability = new int[this.ipBlocks.size()];

    // Cache size of all blocks
    BigInteger count = BigInteger.ZERO;
    for (final IpBlock ipBlock : ipBlocks) {
      count = count.add(ipBlock.getSize());
    }
    this.size = count;
    this.lock = new ReentrantLock();
    this.calculateHitProbabilities();
  }

  private void calculateHitProbabilities() {
    final BigDecimal size = new BigDecimal(this.size);
    final BigInteger sizeMultiplicator = BigInteger.valueOf(Integer.MAX_VALUE); // 100% target = Integer.MAX_VALUE
    for (int i = 0; i < ipBlocks.size(); i++) {
      final IpBlock ipBlock = ipBlocks.get(i);
      final BigInteger calcSize = ipBlock.getSize().multiply(sizeMultiplicator);
      final BigDecimal probability = new BigDecimal(calcSize).divide(size, BigDecimal.ROUND_HALF_UP);
      this.hitProbability[i] = probability.intValue();
    }
  }

  @Override
  public InetAddress getRandomAddress() {
    if (ipBlocks.size() == 1)
      return ipBlocks.get(0).getRandomAddress();
    final int probability = random.nextInt(Integer.MAX_VALUE);
    int probabilitySum = 0;
    int matchIndex = 0;
    for (int i = 0; i < hitProbability.length; i++) {
      if (hitProbability[i] > probability - probabilitySum) {
        matchIndex = i;
        break;
      }
      probabilitySum += hitProbability[i];
    }
    return ipBlocks.get(matchIndex).getRandomAddress();
  }

  @Override
  public InetAddress getAddressAtIndex(BigInteger index) {
    int blockIndex = 0;
    while (index.compareTo(BigInteger.ZERO) > 0) {
      if (ipBlocks.size() <= blockIndex)
        break;
      final IpBlock ipBlock = ipBlocks.get(blockIndex);
      if (ipBlock.getSize().compareTo(index) > 0)
        return ipBlock.getAddressAtIndex(index);
      index = index.subtract(ipBlock.getSize());
      blockIndex++;
    }
    throw new IllegalArgumentException("Index out of bounds for the CombinedBlock");
  }

  @Override
  public Class getType() {
    return this.type;
  }

  @Override
  public BigInteger getSize() {
    return this.size;
  }

  /**
   * Estimates the virtual mask bits of the combined block
   *
   * @return mask bits
   */
  @Override
  public int getMaskBits() {
    int[] bits = new int[getType().equals(Inet6Address.class) ? 128 : 32];
    int maskBits = bits.length;
    try {
      lock.lockInterruptibly();
      for (final IpBlock ipBlock : ipBlocks) {
        final int blockMaskBits = ipBlock.getMaskBits();
        final int bitsAtIndex = bits[blockMaskBits - 1];
        bits[blockMaskBits - 1] = bitsAtIndex + 1;
      }
      lock.unlock();

      for (int i = bits.length - 1; i > 0; i--) {
        final int bitsAtIndex = bits[i];
        final int nextSize = bitsAtIndex / 2;
        bits[i] = bitsAtIndex - nextSize * 2;
        bits[i - 1] = bits[i - 1] + nextSize;
        if (bits[i - 1] > 0)
          maskBits = i;
      }
      return maskBits;
    } catch (final InterruptedException ex) {
      throw new RuntimeException("Could not acquire lock", ex);
    }
  }
}
