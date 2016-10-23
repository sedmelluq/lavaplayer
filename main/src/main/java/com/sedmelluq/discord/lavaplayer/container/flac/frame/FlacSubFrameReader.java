package com.sedmelluq.discord.lavaplayer.container.flac.frame;

import com.sedmelluq.discord.lavaplayer.container.flac.FlacStreamInfo;
import com.sedmelluq.discord.lavaplayer.tools.io.BitStreamReader;

import java.io.IOException;

/**
 * Contains methods for reading a FLAC subframe
 */
public class FlacSubFrameReader {
  private static final Encoding[] encodingMapping = new Encoding[] {
      Encoding.LPC, null, Encoding.FIXED, null, null, Encoding.VERBATIM, Encoding.CONSTANT
  };

  /**
   * Reads and decodes one subframe (a channel of a frame)
   *
   * @param reader Bit stream reader
   * @param streamInfo Stream global info
   * @param frameInfo Current frame info
   * @param sampleBuffer Output buffer for the (possibly delta) decoded samples of this subframe
   * @param channel The index of the current channel
   * @param temporaryBuffer Temporary working buffer of size at least 32
   * @throws IOException On read error
   */
  public static void readSubFrame(BitStreamReader reader, FlacStreamInfo streamInfo, FlacFrameInfo frameInfo,
                                  int[] sampleBuffer, int channel, int[] temporaryBuffer) throws IOException {

    if (reader.asInteger(1) == 1) {
      throw new IllegalStateException("Subframe header must start with 0 bit.");
    }

    boolean isDeltaChannel = frameInfo.channelDelta.deltaChannel == channel;

    int subFrameDescriptor = reader.asInteger(6);

    int wastedBitCount = reader.asInteger(1) == 1 ? reader.readAllZeroes() + 1 : 0;
    int bitsPerSample = streamInfo.bitsPerSample - wastedBitCount + (isDeltaChannel ? 1 : 0);

    readSubFrameSamples(reader, subFrameDescriptor, bitsPerSample, sampleBuffer, frameInfo.sampleCount, temporaryBuffer);

    if (wastedBitCount > 0) {
      for (int i = 0; i < frameInfo.sampleCount; i++) {
        sampleBuffer[i] <<= wastedBitCount;
      }
    }
  }

  private static void readSubFrameSamples(BitStreamReader reader, int subFrameDescriptor, int bitsPerSample, int[] sampleBuffer,
                                          int sampleCount, int[] temporaryBuffer) throws IOException {

    Encoding subframeEncoding = encodingMapping[Integer.numberOfLeadingZeros(subFrameDescriptor) - 26];

    if (subframeEncoding == null) {
      throw new RuntimeException("Invalid subframe type.");
    } else if (subframeEncoding == Encoding.LPC) {
      readSubFrameLpcData(reader, (subFrameDescriptor & 0x1F) + 1, bitsPerSample, sampleBuffer, sampleCount, temporaryBuffer);
    } else if (subframeEncoding == Encoding.FIXED) {
      readSubFrameFixedData(reader, subFrameDescriptor & 0x07, bitsPerSample, sampleBuffer, sampleCount);
    } else if (subframeEncoding == Encoding.VERBATIM) {
      readSubFrameVerbatimData(reader, bitsPerSample, sampleBuffer, sampleCount);
    } else if (subframeEncoding == Encoding.CONSTANT) {
      readSubFrameConstantData(reader, bitsPerSample, sampleBuffer, sampleCount);
    }
  }

  private static void readSubFrameConstantData(BitStreamReader reader, int bitsPerSample, int[] sampleBuffer,
                                               int sampleCount) throws IOException {

    int value = reader.asSignedInteger(bitsPerSample);

    for (int i = 0; i < sampleCount; i++) {
      sampleBuffer[i] = value;
    }
  }

  private static void readSubFrameVerbatimData(BitStreamReader reader, int bitsPerSample, int[] sampleBuffer,
                                               int sampleCount) throws IOException {
    for (int i = 0; i < sampleCount; i++) {
      sampleBuffer[i] = reader.asSignedInteger(bitsPerSample);
    }
  }

  private static void readSubFrameFixedData(BitStreamReader reader, int order, int bitsPerSample, int[] sampleBuffer,
                                            int sampleCount) throws IOException {
    for (int i = 0; i < order; i++) {
      sampleBuffer[i] = reader.asSignedInteger(bitsPerSample);
    }

    readResidual(reader, order, sampleBuffer, order, sampleCount);
    restoreFixedSignal(sampleBuffer, sampleCount, order);
  }

  private static void restoreFixedSignal(int[] buffer, int sampleCount, int order) {
    switch (order) {
      case 1:
        for (int i = order; i < sampleCount; i++) {
          buffer[i] += buffer[i - 1];
        }
        break;
      case 2:
        for (int i = order; i < sampleCount; i++) {
          buffer[i] += (buffer[i - 1] << 1) - buffer[i - 2];
        }
        break;
      case 3:
        for (int i = order; i < sampleCount; i++) {
          buffer[i] += (((buffer[i - 1] - buffer[i - 2]) << 1) + (buffer[i - 1] - buffer[i - 2])) + buffer[i - 3];
        }
        break;
      case 4:
        for (int i = order; i < sampleCount; i++) {
          buffer[i] += ((buffer[i - 1] + buffer[i - 3]) << 2) - ((buffer[i - 2] << 2) + (buffer[i - 2] << 1)) - buffer[i - 4];
        }
        break;
      default:
        break;
    }
  }

  private static void readSubFrameLpcData(BitStreamReader reader, int order, int bitsPerSample, int[] sampleBuffer,
                                          int sampleCount, int[] coefficients) throws IOException {
    for (int i = 0; i < order; i++) {
      sampleBuffer[i] = reader.asSignedInteger(bitsPerSample);
    }

    int precision = reader.asInteger(4) + 1;
    int shift = reader.asInteger(5);

    for (int i = 0; i < order; i++) {
      coefficients[i] = reader.asSignedInteger(precision);
    }

    readResidual(reader, order, sampleBuffer, order, sampleCount);
    restoreLpcSignal(sampleBuffer, sampleCount, order, shift, coefficients);
  }

  private static void restoreLpcSignal(int[] buffer, int sampleCount, int order, int shift, int[] coefficients) {
    for (int i = order; i < sampleCount; i++) {
      long sum = 0;

      for (int j = 0; j < order; j++) {
        sum += (long) coefficients[j] * buffer[i  - j - 1];
      }

      buffer[i] += (int) (sum >> shift);
    }
  }

  private static void readResidual(BitStreamReader reader, int order, int[] buffer, int startOffset, int endOffset) throws IOException {
    int method = reader.asInteger(2);

    if (method > 1) {
      throw new RuntimeException("Invalid residual coding method " + method);
    }

    int partitionOrder = reader.asInteger(4);
    int partitions = 1 << partitionOrder;
    int partitionSamples = partitionOrder > 0 ? endOffset >> partitionOrder : endOffset - order;
    int parameterLength = method == 0 ? 4 : 5;
    int parameterMaximum = (1 << parameterLength) - 1;

    int sample = startOffset;

    for (int partition = 0; partition < partitions; partition++) {
      int parameter = reader.asInteger(parameterLength);
      int value = (partitionOrder == 0 || partition > 0) ? 0 : order;

      if (parameter < parameterMaximum) {
        value = partitionSamples - value;
        readResidualBlock(reader, buffer, sample, sample + value, parameter);
        sample += value;
      } else {
        parameter = reader.asInteger(5);

        for (int i = value ; i < partitionSamples; i++, sample++) {
          buffer[sample] = reader.asSignedInteger(parameter);
        }
      }
    }
  }

  private static void readResidualBlock(BitStreamReader reader, int[] buffer, int offset, int endOffset, int parameter) throws IOException {
    for (int i = offset; i < endOffset; i++) {
      int lowOrderSigned = (reader.readAllZeroes() << parameter) | reader.asInteger(parameter);
      buffer[i] = (lowOrderSigned & 1) == 0 ? lowOrderSigned >> 1 : -(lowOrderSigned >> 1) - 1;
    }
  }

  private enum Encoding {
    CONSTANT,
    VERBATIM,
    FIXED,
    LPC
  }
}
