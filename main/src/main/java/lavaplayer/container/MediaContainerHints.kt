package lavaplayer.container

/**
 * Optional meta-information about a stream which may narrow down the list of possible containers.
 */
data class MediaContainerHints internal constructor(
    /**
     * Mime type, null if not known.
     */
    @JvmField val mimeType: String?,
    /**
     * File extension, null if not known.
     */
    @JvmField val fileExtension: String?
) {
    companion object {
        private val NO_INFORMATION = MediaContainerHints(null, null)

        /**
         * @param mimeType      Mime type
         * @param fileExtension File extension
         * @return Instance of hints object with the specified parameters
         */
        @JvmStatic
        fun from(mimeType: String?, fileExtension: String?): MediaContainerHints {
            return if (mimeType == null && fileExtension == null) {
                NO_INFORMATION
            } else {
                MediaContainerHints(mimeType, fileExtension)
            }
        }
    }

    /**
     * @return `true` if any hint parameters have a value.
     */
    val isPresent: Boolean
        get() = mimeType != null || fileExtension != null
}
