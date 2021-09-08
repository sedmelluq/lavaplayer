package lavaplayer.source.youtube

import lavaplayer.source.youtube.YoutubeCipherOperationType.*

/**
 * Describes one signature cipher
 */
class YoutubeSignatureCipher {
    private val operations = mutableListOf<YoutubeCipherOperation>()

    var scriptTimestamp = ""

    /**
     * @return True if the cipher contains no operations.
     */
    val isEmpty: Boolean
        get() = operations.isEmpty()

    /**
     * @param text Text to apply the cipher on
     * @return The result of the cipher on the input text
     */
    fun apply(text: String): String {
        val builder = StringBuilder(text)
        for (operation in operations) {
            when (operation.type) {
                SWAP -> {

                    val position = operation.parameter % text.length
                    val temp = builder[0]
                    builder.setCharAt(0, builder[position])
                    builder.setCharAt(position, temp)
                }
                REVERSE -> builder.reverse()
                SLICE, SPLICE -> builder.delete(0, operation.parameter)
            }
        }

        return builder.toString()
    }

    /**
     * @param operation The operation to add to this cipher
     */
    fun addOperation(operation: YoutubeCipherOperation) {
        operations.add(operation)
    }
}
