package dev.videobridge.onvif;

import java.util.List;

public final class OnvifModels {

    private OnvifModels() {
    }

    public record DeviceInfo(String manufacturer, String model, String firmwareVersion,
                             String serialNumber, String hardwareId) {
    }

    public record MediaProfile(String token, String name, String sourceToken,
                               String encoding, String resolution) {
    }

    public record VideoSource(String token, String name) {
    }

    public record Preset(String token, String name) {
    }

    public record HikChannel(String id, String name, String ip, String port, String status) {
    }

    public record RecordSegment(String startTime, String endTime, long durationSeconds, int channelNo) {
    }

    public record DiscoveredDevice(String name, List<MediaProfile> profiles, List<VideoSource> sources) {
    }
}
