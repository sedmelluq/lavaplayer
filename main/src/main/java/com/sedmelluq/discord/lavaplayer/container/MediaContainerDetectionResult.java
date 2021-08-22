package com.sedmelluq.discord.lavaplayer.container;

import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

/**
 * Result of audio container detection.
 */
public class MediaContainerDetectionResult {
    private static final MediaContainerDetectionResult UNKNOWN_FORMAT =
        new MediaContainerDetectionResult(null, null, null, null, null);

    private final AudioTrackInfo trackInfo;
    private final MediaContainerProbe containerProbe;
    private final String probeSettings;
    private final AudioReference reference;
    private final String unsupportedReason;

    private MediaContainerDetectionResult(AudioTrackInfo trackInfo, MediaContainerProbe containerProbe,
                                          String probeSettings, AudioReference reference, String unsupportedReason) {

        this.trackInfo = trackInfo;
        this.containerProbe = containerProbe;
        this.probeSettings = probeSettings;
        this.reference = reference;
        this.unsupportedReason = unsupportedReason;
    }

    /**
     * Creates an unknown format result.
     */
    public static MediaContainerDetectionResult unknownFormat() {
        return UNKNOWN_FORMAT;
    }

    /**
     * Creates a result ofr an unsupported file of a known container.
     *
     * @param probe  Probe of the container
     * @param reason The reason why this track is not supported
     */
    public static MediaContainerDetectionResult unsupportedFormat(MediaContainerProbe probe, String reason) {
        return new MediaContainerDetectionResult(null, probe, null, null, reason);
    }

    /**
     * Creates a load result referring to another item.
     *
     * @param probe     Probe of the container
     * @param reference Reference to another item
     */
    public static MediaContainerDetectionResult refer(MediaContainerProbe probe, AudioReference reference) {
        return new MediaContainerDetectionResult(null, probe, null, reference, null);
    }


    /**
     * Creates a load result for supported file.
     *
     * @param probe     Probe of the container
     * @param trackInfo Track info for the file
     */
    public static MediaContainerDetectionResult supportedFormat(MediaContainerProbe probe, String settings,
                                                                AudioTrackInfo trackInfo) {

        return new MediaContainerDetectionResult(trackInfo, probe, settings, null, null);
    }

    /**
     * @return If the container this file uses was detected. In case this returns true, the container probe is non-null.
     */
    public boolean isContainerDetected() {
        return containerProbe != null;
    }

    /**
     * @return The probe for the container of the file
     */
    public MediaContainerDescriptor getContainerDescriptor() {
        return new MediaContainerDescriptor(containerProbe, probeSettings);
    }

    /**
     * @return Whether this specific file is supported. If this returns true, the track info is non-null. Otherwise
     * the reason why this file is not supported can be retrieved via getUnsupportedReason().
     */
    public boolean isSupportedFile() {
        return isContainerDetected() && unsupportedReason == null;
    }

    /**
     * @return The reason why this track is not supported.
     */
    public String getUnsupportedReason() {
        return unsupportedReason;
    }

    /**
     * @return Track info for the detected file.
     */
    public AudioTrackInfo getTrackInfo() {
        return trackInfo;
    }

    public boolean isReference() {
        return reference != null;
    }

    public AudioReference getReference() {
        return reference;
    }
}
