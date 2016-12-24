package com.sedmelluq.discord.lavaplayer.udpqueue.natives;

import com.sedmelluq.discord.lavaplayer.natives.NativeLibLoader;

import java.nio.ByteBuffer;

class UdpQueueManagerLibrary {
  private UdpQueueManagerLibrary() {

  }

  static UdpQueueManagerLibrary getInstance() {
    NativeLibLoader.load(UdpQueueManagerLibrary.class, "udpqueue");
    return new UdpQueueManagerLibrary();
  }

  native long create(int bufferCapacity, long packetInterval);

  native void destroy(long instance);

  native int getRemainingCapacity(long instance, long key);

  native boolean queuePacket(long instance, long key, String address, int port, ByteBuffer dataDirectBuffer, int dataLength);

  native void process(long instance);

  static native void pauseDemo(int length);
}
