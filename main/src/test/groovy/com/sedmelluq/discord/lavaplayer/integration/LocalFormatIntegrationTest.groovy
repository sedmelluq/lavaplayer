package com.sedmelluq.discord.lavaplayer.integration

import com.sedmelluq.discord.lavaplayer.format.Pcm16AudioDataFormat
import com.sedmelluq.discord.lavaplayer.integration.LocalFormatSampleIndex.Sample
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import org.apache.commons.io.IOUtils
import spock.lang.Specification
import spock.lang.Timeout
import spock.lang.Unroll

import java.nio.ByteBuffer
import java.nio.file.Files

import static com.sedmelluq.discord.lavaplayer.integration.PlayerManagerTestTools.consumeTrack
import static com.sedmelluq.discord.lavaplayer.integration.PlayerManagerTestTools.loadTrack

@Timeout(30)
class LocalFormatIntegrationTest extends Specification {
  static AudioPlayerManager manager
  static File temporaryDirectory

  def setupSpec() {
    manager = new DefaultAudioPlayerManager()
    AudioSourceManagers.registerLocalSource(manager)

    temporaryDirectory = Files.createTempDirectory('lavaplayer-test-samples').toFile()

    LocalFormatSampleIndex.SAMPLES.each {
      new File(temporaryDirectory, it.filename).withOutputStream { out ->
        LocalFormatIntegrationTest.class.getResourceAsStream("/test-samples/" + it.filename).withCloseable { input ->
          IOUtils.copy(input, out)
        }
      }
    }
  }

  def cleanupSpec() {
    manager.shutdown()

    temporaryDirectory.deleteDir()
  }

  @Unroll
  def "decoding produces expected hash for #sample"(Sample sample) {
    manager.configuration.outputFormat = new Pcm16AudioDataFormat(2, sample.getSampleRate(), 960, false)
    AudioPlayer player = manager.createPlayer()
    ByteBuffer buffer = ByteBuffer.allocate(960 * 2 * 2)

    MutableAudioFrame frame = new MutableAudioFrame()
    frame.setBuffer(buffer)

    List<AudioEvent> events = []

    player.addListener(new AudioEventListener() {
      @Override
      void onEvent(AudioEvent event) {
        events.add(event)
      }
    })

    player.playTrack(loadTrack(manager, temporaryDirectory.absolutePath + "/" + sample.filename))

    expect:
    sample.validCrcs.contains(consumeTrack(player))
    events.size() == 2
    events.get(0) instanceof TrackStartEvent
    events.get(1) instanceof TrackEndEvent

    where:
    sample << LocalFormatSampleIndex.SAMPLES
  }
}
