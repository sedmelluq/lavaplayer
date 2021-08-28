package lavaplayer.container.ogg;

public interface OggTrackBlueprint {
    OggTrackHandler loadTrackHandler(OggPacketInputStream stream);
}
