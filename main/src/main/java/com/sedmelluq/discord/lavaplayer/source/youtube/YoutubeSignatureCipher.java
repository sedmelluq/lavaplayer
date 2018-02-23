package com.sedmelluq.discord.lavaplayer.source.youtube;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes one signature cipher
 */
public class YoutubeSignatureCipher {
  private final List<YoutubeCipherOperation> operations = new ArrayList<>();

  /**
   * @param text Text to apply the cipher on
   * @return The result of the cipher on the input text
   */
  public String apply(String text) {
    StringBuilder builder = new StringBuilder(text);

    for (YoutubeCipherOperation operation : operations) {
      switch (operation.type) {
        case SWAP:
          int position = operation.parameter % text.length();
          char temp = builder.charAt(0);
          builder.setCharAt(0, builder.charAt(position));
          builder.setCharAt(position, temp);
          break;
        case REVERSE:
          builder.reverse();
          break;
        case SLICE:
        case SPLICE:
          builder.delete(0, operation.parameter);
          break;
        default:
          throw new IllegalStateException("All branches should be covered");
      }
    }

    return builder.toString();
  }

  /**
   * @param operation The operation to add to this cipher
   */
  public void addOperation(YoutubeCipherOperation operation) {
    operations.add(operation);
  }

  /**
   * @return True if the cipher contains no operations.
   */
  public boolean isEmpty() {
    return operations.isEmpty();
  }
}
