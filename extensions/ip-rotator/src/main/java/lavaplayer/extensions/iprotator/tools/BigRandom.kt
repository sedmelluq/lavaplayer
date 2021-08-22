package lavaplayer.extensions.iprotator.tools

import java.math.BigInteger
import java.util.*

class BigRandom : Random() {

    fun nextBigInt(bits: Int): BigInteger {
        if (bits < 32) {
            return next(31).toBigInteger();
        }

        var bits = bits
        var value = BigInteger.ZERO
        var index = 0
        while (bits >= 32) {
            bits -= 32;
            value = value.add(next(32).toBigInteger() shl (index++) * 32);
        }

        if (bits > 0) {
            value = value.add(next(bits).toBigInteger() shl index * 32);
        }

        return value;
    }

}
