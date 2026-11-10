package dev.videobridge.storage;

import dev.videobridge.core.VideoProtocol;
import dev.videobridge.onvif.OnvifDevice;

import java.time.LocalDateTime;

public record Channel(
        Long id,
        String channelKey,
        String name,
        VideoProtocol protocol,
        String gbDeviceId,
        String gbChannelId,
        String onvifHost,
        Integer onvifPort,
        String onvifUsername,
        String onvifPassword,
        String nvrHost,
        Integer channelNo,
        String vendor,
        boolean ptzEnabled,
        boolean permanent,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public OnvifDevice onvifDevice() {
        return new OnvifDevice(onvifHost, onvifPort == null ? 80 : onvifPort,
                onvifUsername, onvifPassword);
    }

    public String streamKey() {
        if (protocol == VideoProtocol.ONVIF) {
            return channelKey;
        }
        return gbDeviceId + "_" + gbChannelId;
    }

    public String nvrHostOrSelf() {
        return nvrHost == null || nvrHost.isBlank() ? onvifHost : nvrHost;
    }
}
