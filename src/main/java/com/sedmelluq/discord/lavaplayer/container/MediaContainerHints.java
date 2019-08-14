package com.sedmelluq.discord.lavaplayer.container;

/**
 * Optional meta-information about a stream which may narrow down the list of possible containers.
 */
public class MediaContainerHints {
  private static final MediaContainerHints NO_INFORMATION = new MediaContainerHints(null, null);

  /**
   * Mime type, null if not known.
   */
  public final String mimeType;
  /**
   * File extension, null if not known.
   */
  public final String fileExtension;

  private MediaContainerHints(String mimeType, String fileExtension) {
    this.mimeType = mimeType;
    this.fileExtension = fileExtension;
  }

  /**
   * @return <code>true</code> if any hint parameters have a value.
   */
  public boolean present() {
    return mimeType != null || fileExtension != null;
  }

  /**
   * @param mimeType Mime type
   * @param fileExtension File extension
   * @return Instance of hints object with the specified parameters
   */
  public static MediaContainerHints from(String mimeType, String fileExtension) {
    if (mimeType == null && fileExtension == null) {
      return NO_INFORMATION;
    } else {
      return new MediaContainerHints(mimeType, fileExtension);
    }
  }
}
