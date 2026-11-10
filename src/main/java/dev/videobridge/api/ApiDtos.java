package dev.videobridge.api;

import dev.videobridge.core.VideoProtocol;
import dev.videobridge.onvif.OnvifDevice;
import dev.videobridge.storage.Channel;
import jakarta.validation.constraints.NotBlank;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record ChannelRequest(
            @NotBlank String channelKey,
            String name,
            String protocol,
            String gbDeviceId,
            String gbChannelId,
            String onvifHost,
            Integer onvifPort,
            String onvifUsername,
            String onvifPassword,
            String nvrHost,
            Integer channelNo,
            String vendor,
            Boolean ptzEnabled,
            Boolean permanent,
            Boolean enabled) {

        public Channel toChannel() {
            VideoProtocol parsed = VideoProtocol.parse(protocol);
            if (parsed == VideoProtocol.ONVIF && (onvifHost == null || onvifHost.isBlank())) {
                throw new IllegalArgumentException("ONVIF 通道必须提供 onvifHost");
            }
            if (parsed == VideoProtocol.GB28181
                    && (gbDeviceId == null || gbChannelId == null)) {
                throw new IllegalArgumentException("国标通道必须提供 gbDeviceId 和 gbChannelId");
            }
            return new Channel(null, channelKey, name, parsed, gbDeviceId, gbChannelId,
                    onvifHost, onvifPort, onvifUsername, onvifPassword, nvrHost, channelNo,
                    vendor == null ? "AUTO" : vendor,
                    ptzEnabled == null || ptzEnabled,
                    permanent != null && permanent,
                    enabled == null || enabled, null, null);
        }
    }

    public record PtzRequest(@NotBlank String channelKey, @NotBlank String command, Float speed) {
    }

    public record PresetRequest(@NotBlank String channelKey, @NotBlank String command,
                                String presetToken, String name) {
    }

    public record PlaybackRequest(@NotBlank String channelKey, @NotBlank String startTime,
                                  @NotBlank String endTime, Boolean download) {
    }

    public record ControlRequest(@NotBlank String streamKey, @NotBlank String type,
                                 Long seek, Double speed) {
    }

    public record OnvifProbe(@NotBlank String host, Integer port,
                             @NotBlank String username, @NotBlank String password) {
        public OnvifDevice toDevice() {
            return new OnvifDevice(host, port == null ? 80 : port, username, password);
        }
    }
}
