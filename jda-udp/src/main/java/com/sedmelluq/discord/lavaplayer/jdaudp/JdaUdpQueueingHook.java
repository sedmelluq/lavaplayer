package com.sedmelluq.discord.lavaplayer.jdaudp;

import com.sedmelluq.discord.lavaplayer.player.hook.AudioOutputHook;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.udpqueue.natives.UdpQueueManager;
import net.dv8tion.jda.audio.AudioConnection;
import net.dv8tion.jda.audio.AudioPacket;
import net.dv8tion.jda.audio.AudioWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramPacket;

/**
 * Audio output hook to redirect audio packets to the native UDP queue manager library instead of the packets being
 * sent by JDA itself. Involves a bit of hackery, but there is no pretty way to do this for now.
 */
public class JdaUdpQueueingHook implements AudioOutputHook {
  private static final int PACKET_SAMPLE_COUNT = 960;
  private static final int FIRST_PACKET_INDEX = 10;

  private static final Logger log = LoggerFactory.getLogger(JdaUdpQueueingHook.class);

  private final UdpQueueManager udpQueueManager;
  private boolean speaking;
  private int sequence;
  private int timestamp;
  private Thread activeThread;
  private AudioConnection activeConnection;
  private AudioWebSocket activeAudioWebSocket;
  private Method setSpeakingMethod;

  /**
   * @param udpQueueManager Queue manager
   */
  public JdaUdpQueueingHook(UdpQueueManager udpQueueManager) {
    this.udpQueueManager = udpQueueManager;
    this.timestamp = PACKET_SAMPLE_COUNT * FIRST_PACKET_INDEX;
    this.sequence = FIRST_PACKET_INDEX;
    this.speaking = false;
  }

  @Override
  public AudioFrame outgoingFrame(AudioPlayer player, AudioFrame firstFrame) {
    boolean timestampIncreased = false;

    try {
      initialiseReflection();

      if (firstFrame == null) {
        setSpeaking(false);
        return null;
      }

      setSpeaking(true);

      long key = System.identityHashCode(activeConnection);
      int maximum = Math.max(udpQueueManager.getRemainingCapacity(key) - 2, 1);

      for (int i = 0; i < maximum; i++) {
        AudioFrame current = i == 0 ? firstFrame : player.provideDirectly();

        if (current == null) {
          break;
        }

        timestampIncreased = true;
        queuePacketForFrame(current, key);
      }
    } catch (Exception e) {
      log.error("Error when queueing UDP packets.", e);
    } finally {
      if (!timestampIncreased) {
        timestamp += PACKET_SAMPLE_COUNT;
      }
    }

    return null;
  }

  private void queuePacketForFrame(AudioFrame frame, long key) {
    timestamp += PACKET_SAMPLE_COUNT;

    AudioPacket packet = new AudioPacket((char) sequence, timestamp, activeAudioWebSocket.getSSRC(), frame.data);
    DatagramPacket datagramPacket = packet.asEncryptedUdpPacket(activeAudioWebSocket.getAddress(), activeAudioWebSocket.getSecretKey());

    if (!udpQueueManager.queuePacket(key, datagramPacket)) {
      log.debug("Queue manager refused a packet.");
    }

    sequence = sequence + 1 > Character.MAX_VALUE ? 0 : sequence + 1;
  }

  private void initialiseReflection() throws Exception {
    Thread currentThread = Thread.currentThread();

    if (activeThread == currentThread) {
      return;
    }

    activeThread = currentThread;

    Field audioConnectionField = Thread.currentThread().getClass().getDeclaredField("this$0");
    audioConnectionField.setAccessible(true);

    activeConnection = (AudioConnection) audioConnectionField.get(Thread.currentThread());

    Field audioSocketField = activeConnection.getClass().getDeclaredField("webSocket");
    audioSocketField.setAccessible(true);

    activeAudioWebSocket = (AudioWebSocket) audioSocketField.get(activeConnection);

    setSpeakingMethod = activeConnection.getClass().getDeclaredMethod("setSpeaking", boolean.class);
    setSpeakingMethod.setAccessible(true);
  }

  private void setSpeaking(boolean state) throws Exception {
    if (speaking != state) {
      setSpeakingMethod.invoke(activeConnection, state);
      speaking = state;
    }
  }
}
