package lavaplayer.container.ogg

interface OggTrackBlueprint {
    fun loadTrackHandler(stream: OggPacketInputStream): OggTrackHandler
}
