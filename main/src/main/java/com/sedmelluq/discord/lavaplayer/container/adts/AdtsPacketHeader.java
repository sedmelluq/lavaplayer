package com.sedmelluq.discord.lavaplayer.container.adts;

/**
 * ADTS packet header.
 */
public class AdtsPacketHeader {
  /**
   * If this is false, then the packet header is followed by a 2-byte CRC.
   */
  public final boolean isProtectionAbsent;
  /**
   * Decoder profile (2 is AAC-LC).
   */
  public final int profile;
  /**
   * Sample rate.
   */
  public final int sampleRate;
  /**
   * Number of channels.
   */
  public final int channels;
  /**
   * Packet payload length, excluding the CRC after this header.
   */
  public final int payloadLength;

  /**
   * @param isProtectionAbsent If this is false, then the packet header is followed by a 2-byte CRC.
   * @param profile Decoder profile (2 is AAC-LC).
   * @param sampleRate Sample rate.
   * @param channels Number of channels.
   * @param payloadLength Packet payload length, excluding the CRC after this header.
   */
  public AdtsPacketHeader(boolean isProtectionAbsent, int profile, int sampleRate, int channels, int payloadLength) {
    this.isProtectionAbsent = isProtectionAbsent;
    this.profile = profile;
    this.sampleRate = sampleRate;
    this.channels = channels;
    this.payloadLength = payloadLength;
  }

  /**
   * @param packetHeader The packet to check against.
   * @return True if the decoder does not have to be reconfigured between these two packets.
   */
  public boolean canUseSameDecoder(AdtsPacketHeader packetHeader) {
    return packetHeader != null &&
        profile == packetHeader.profile &&
        sampleRate == packetHeader.sampleRate &&
        channels == packetHeader.channels;
  }
}
