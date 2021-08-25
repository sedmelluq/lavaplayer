package lavaplayer.tools

import java.util.function.Function

/**
 * Utility class for calculating averages on the last N values, with input and output transformers.
 *
 * @param size            Maximum number of values to remember.
 * @param inputProcessor  Input transformer.
 * @param outputProcessor Output transformer.
 */
class RingBufferMath(
    private var size: Int,
    private val inputProcessor: Function<Double, Double>,
    private val outputProcessor: Function<Double, Double>
) {
    private val values = DoubleArray(size)
    private var sum = 0.0
    private var position = 0

    /**
     * @param value Original value to add (before transformation)
     */
    fun add(value: Double) {
        var value = value
        value = inputProcessor.apply(value)
        sum -= values[position]
        values[position] = value
        sum += values[position]
        position = if (position + 1 == values.size) 0 else position + 1
        size = values.size.coerceAtMost(size + 1)
    }

    /**
     * @return Transformed mean of the internal values.
     */
    fun mean(): Double {
        return outputProcessor.apply(if (size == 0) 0.0 else sum / size)
    }
}
