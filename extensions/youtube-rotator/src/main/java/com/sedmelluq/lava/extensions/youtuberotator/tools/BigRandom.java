package com.sedmelluq.lava.extensions.youtuberotator.tools;

import java.math.BigInteger;
import java.util.Random;

public final class BigRandom extends Random {

  public BigInteger nextBigInt(int bits) {
    if (bits < 32) {
      return BigInteger.valueOf(next(31));
    }
    BigInteger value = BigInteger.ZERO;
    int index = 0;
    while (bits >= 32) {
      bits -= 32;
      value = value.add(BigInteger.valueOf(next(32)).shiftLeft((index++) * 32));
    }
    if (bits > 0)
      value = value.add(BigInteger.valueOf(next(bits)).shiftLeft(index * 32));
    return value;
  }

}
