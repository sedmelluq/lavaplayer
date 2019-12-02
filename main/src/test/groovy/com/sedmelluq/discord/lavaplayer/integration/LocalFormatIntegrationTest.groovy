package com.sedmelluq.discord.lavaplayer.integration

import com.sedmelluq.discord.lavaplayer.integration.LocalFormatSampleIndex.Sample
import com.sedmelluq.lavaplayer.core.format.Pcm16AudioDataFormat
import com.sedmelluq.lavaplayer.core.manager.AudioPlayerManager
import com.sedmelluq.lavaplayer.core.manager.DefaultAudioPlayerManager
import com.sedmelluq.lavaplayer.core.player.AudioPlayer
import com.sedmelluq.lavaplayer.core.player.AudioTrackRequestBuilder
import com.sedmelluq.lavaplayer.core.player.event.AudioPlayerEvent
import com.sedmelluq.lavaplayer.core.player.event.AudioPlayerEventListener
import com.sedmelluq.lavaplayer.core.player.event.TrackEndEvent
import com.sedmelluq.lavaplayer.core.player.event.TrackStartEvent
import com.sedmelluq.lavaplayer.core.player.frame.MutableAudioFrame
import com.sedmelluq.lavaplayer.core.source.AudioSourceManagers
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
    manager = DefaultAudioPlayerManager.createDefault()
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
    manager.close()

    temporaryDirectory.deleteDir()
  }

  @Unroll
  def "decoding produces expected hash for #sample"(Sample sample) {
    manager.configuration.outputFormat = new Pcm16AudioDataFormat(2, sample.getSampleRate(), 960, false)
    AudioPlayer player = manager.createPlayer()
    ByteBuffer buffer = ByteBuffer.allocate(960 * 2 * 2)

    MutableAudioFrame frame = new MutableAudioFrame()
    frame.setBuffer(buffer)

    List<AudioPlayerEvent> events = []

    player.addListener(new AudioPlayerEventListener() {
      @Override
      void onEvent(AudioPlayerEvent event) {
        events.add(event)
      }
    })

    player.playTrack(new AudioTrackRequestBuilder(loadTrack(manager, temporaryDirectory.absolutePath + "/" + sample.filename)))

    expect:
    sample.validCrcs.contains(consumeTrack(player))
    events.size() == 2
    events.get(0) instanceof TrackStartEvent
    events.get(1) instanceof TrackEndEvent

    where:
    sample << LocalFormatSampleIndex.SAMPLES
  }
}
