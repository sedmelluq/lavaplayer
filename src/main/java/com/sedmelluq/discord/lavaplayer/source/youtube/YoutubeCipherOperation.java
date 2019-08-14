package com.sedmelluq.discord.lavaplayer.source.youtube;

/**
 * One cipher operation definition.
 */
public class YoutubeCipherOperation {
  /**
   * The type of the operation.
   */
  public final YoutubeCipherOperationType type;
  /**
   * The parameter for the operation.
   */
  public final int parameter;

  /**
   * @param type The type of the operation.
   * @param parameter The parameter for the operation.
   */
  public YoutubeCipherOperation(YoutubeCipherOperationType type, int parameter) {
    this.type = type;
    this.parameter = parameter;
  }
}
