package lavaplayer.tools

object Units {
    /**
     * Not a negative number, so that we would not need to test for it separately when comparing.
     */
    const val CONTENT_LENGTH_UNKNOWN = Long.MAX_VALUE
    const val DURATION_MS_UNKNOWN = Long.MAX_VALUE
    const val DURATION_SEC_UNKNOWN = Long.MAX_VALUE
    const val BITRATE_UNKNOWN: Long = -1

    private const val SECONDS_MAXIMUM = DURATION_SEC_UNKNOWN / 1000

    @JvmStatic
    fun secondsToMillis(seconds: Long): Long {
        return if (seconds == DURATION_SEC_UNKNOWN) {
            DURATION_MS_UNKNOWN
        } else if (seconds > SECONDS_MAXIMUM) {
            throw RuntimeException("Cannot convert $seconds to millis - would overflow.")
        } else {
            seconds * 1000
        }
    }
}
