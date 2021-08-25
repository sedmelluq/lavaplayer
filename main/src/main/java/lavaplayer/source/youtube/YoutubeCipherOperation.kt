package lavaplayer.source.youtube

/**
 * One cipher operation definition.
 *
 * @param type      The type of the operation.
 * @param parameter The parameter for the operation.
 */
data class YoutubeCipherOperation(
    /**
     * The type of the operation.
     */
    @JvmField val type: YoutubeCipherOperationType,
    /**
     * The parameter for the operation.
     */
    @JvmField val parameter: Int
)
