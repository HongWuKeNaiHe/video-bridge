package dev.videobridge.onvif;

import dev.videobridge.common.BridgeException;
import dev.videobridge.common.Http;
import dev.videobridge.common.Xml;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class OnvifClient {

    private static final String SCOPE_NAME_PREFIX = "onvif://www.onvif.org/name/";

    private final OnvifTransport transport;

    public OnvifClient(OnvifTransport transport) {
        this.transport = transport;
    }

    public OnvifModels.DeviceInfo deviceInfo(OnvifDevice device) {
        String xml = checked(transport.soap(device, OnvifPaths.DEVICE, Soap.deviceInfo()));
        return new OnvifModels.DeviceInfo(
                text(xml, "Manufacturer"), text(xml, "Model"), text(xml, "FirmwareVersion"),
                text(xml, "SerialNumber"), text(xml, "HardwareId"));
    }

    public List<OnvifModels.MediaProfile> profiles(OnvifDevice device) {
        String xml = checked(transport.soap(device, OnvifPaths.MEDIA, Soap.getProfiles()));
        List<OnvifModels.MediaProfile> result = new ArrayList<>();
        for (String block : Xml.blocks(xml, "Profiles")) {
            String token = Xml.attr(block, "Profiles", "token");
            if (token == null) {
                continue;
            }
            String width = text(block, "Width");
            String height = text(block, "Height");
            String resolution = width == null ? "" : width + "x" + height;
            result.add(new OnvifModels.MediaProfile(token, text(block, "Name"),
                    text(block, "SourceToken"), text(block, "Encoding"), resolution));
        }
        return result;
    }

    public List<OnvifModels.VideoSource> videoSources(OnvifDevice device) {
        String xml = checked(transport.soap(device, OnvifPaths.MEDIA, Soap.getVideoSources()));
        List<OnvifModels.VideoSource> result = new ArrayList<>();
        // 有的固件用 VideoSources，有的用 VideoSource，两种都收
        List<String> blocks = Xml.blocks(xml, "VideoSources");
        if (blocks.isEmpty()) {
            blocks = Xml.blocks(xml, "VideoSource");
        }
        for (String block : blocks) {
            String token = Xml.attr(block, "VideoSources", "token");
            if (token == null) {
                token = Xml.attr(block, "VideoSource", "token");
            }
            if (token != null) {
                result.add(new OnvifModels.VideoSource(token, text(block, "Name")));
            }
        }
        return result;
    }

    public String deviceName(OnvifDevice device) {
        String hostname = checked(transport.soap(device, OnvifPaths.DEVICE, Soap.getHostname()));
        String name = text(hostname, "Name");
        if (name != null && !name.isEmpty()) {
            return name;
        }
        String scopes = checked(transport.soap(device, OnvifPaths.DEVICE, Soap.getScopes()));
        for (String item : Xml.texts(scopes, "ScopeItem")) {
            if (item.startsWith(SCOPE_NAME_PREFIX)) {
                String encoded = item.substring(SCOPE_NAME_PREFIX.length());
                return encoded.isEmpty() ? null : URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    public String streamUri(OnvifDevice device, String profileToken) {
        String token = requireProfile(device, profileToken);
        String xml = checked(transport.soap(device, OnvifPaths.MEDIA, Soap.getStreamUri(token)));
        return embedAuth(text(xml, "Uri"), device);
    }

    /** 标准 ReplayControl，只在按厂商规则拼地址拉不通时再试 */
    public String standardReplayUri(OnvifDevice device, String profileToken, String startTime, String endTime) {
        String start = ReplayUriBuilder.standard(startTime);
        String end = ReplayUriBuilder.standard(endTime);
        if (start == null || end == null) {
            throw new IllegalArgumentException("回放时间格式应为 yyyy-MM-dd HH:mm:ss");
        }
        String token = requireProfile(device, profileToken);
        String xml = checked(transport.soap(device, OnvifPaths.REPLAY,
                Soap.getReplayUri(token, start, end)));
        return embedAuth(text(xml, "Uri"), device);
    }

    public boolean ptz(OnvifDevice device, String profileToken, String command, float speed) {
        String token = requireProfile(device, profileToken);
        float s = (speed > 0 && speed <= 1) ? speed : 0.5f;
        String cmd = command == null ? "stop" : command.toLowerCase(Locale.ROOT);
        String soap = switch (cmd) {
            case "left" -> Soap.continuousMove(token, -s, 0, 0);
            case "right" -> Soap.continuousMove(token, s, 0, 0);
            case "up" -> Soap.continuousMove(token, 0, s, 0);
            case "down" -> Soap.continuousMove(token, 0, -s, 0);
            case "zoomin" -> Soap.continuousMove(token, 0, 0, s);
            case "zoomout" -> Soap.continuousMove(token, 0, 0, -s);
            case "stop" -> Soap.stop(token);
            default -> throw new IllegalArgumentException("不支持的云台指令: " + command);
        };
        // 控制类响应基本是空 Body，只要不是 Fault 就认为指令已经送到设备
        checked(transport.soap(device, OnvifPaths.PTZ, soap));
        return true;
    }

    public List<OnvifModels.Preset> presets(OnvifDevice device, String profileToken) {
        String token = requireProfile(device, profileToken);
        String xml = checked(transport.soap(device, OnvifPaths.PTZ, Soap.getPresets(token)));
        List<OnvifModels.Preset> result = new ArrayList<>();
        for (String block : Xml.blocks(xml, "Preset")) {
            String presetToken = Xml.attr(block, "Preset", "token");
            if (presetToken != null) {
                result.add(new OnvifModels.Preset(presetToken, text(block, "Name")));
            }
        }
        return result;
    }

    /** set 返回设备分配的预置位号，其余操作返回 "ok" */
    public String preset(OnvifDevice device, String profileToken, String command,
                         String presetToken, String name) {
        String token = requireProfile(device, profileToken);
        String cmd = command == null ? "" : command.toLowerCase(Locale.ROOT);
        String soap = switch (cmd) {
            case "set" -> Soap.setPreset(token, null, name);
            case "update" -> Soap.setPreset(token, requirePresetToken(presetToken), name);
            case "goto" -> Soap.gotoPreset(token, requirePresetToken(presetToken));
            case "remove" -> Soap.removePreset(token, requirePresetToken(presetToken));
            default -> throw new IllegalArgumentException("不支持的预置位操作: " + command);
        };
        String xml = checked(transport.soap(device, OnvifPaths.PTZ, soap));
        if ("set".equals(cmd)) {
            String assigned = text(xml, "PresetToken");
            return assigned == null ? "ok" : assigned;
        }
        return "ok";
    }

    public OnvifModels.DiscoveredDevice discover(OnvifDevice device) {
        return new OnvifModels.DiscoveredDevice(deviceName(device), profiles(device), videoSources(device));
    }

    private String requireProfile(OnvifDevice device, String token) {
        if (token != null && !token.isBlank()) {
            return token;
        }
        List<OnvifModels.MediaProfile> profiles = profiles(device);
        if (profiles.isEmpty()) {
            throw new BridgeException("设备上没有可用的 Media Profile");
        }
        return profiles.get(0).token();
    }

    private static String requirePresetToken(String presetToken) {
        if (presetToken == null || presetToken.isBlank()) {
            throw new IllegalArgumentException("该操作需要 presetToken");
        }
        return presetToken;
    }

    private static String text(String xml, String tag) {
        return Xml.text(xml, tag);
    }

    private static String embedAuth(String uri, OnvifDevice device) {
        if (uri == null || !uri.startsWith("rtsp://") || uri.contains("@")) {
            return uri;
        }
        return "rtsp://" + Http.encode(device.username()) + ":" + Http.encode(device.password())
                + "@" + uri.substring(7);
    }

    // 设备经常把错误放在 HTTP 200 的 SOAP Fault 里，不检查会把空响应当成功
    private static String checked(String xml) {
        if (xml == null) {
            throw new BridgeException("设备没有返回内容");
        }
        if (xml.contains("Fault")) {
            String reason = Xml.text(xml, "Text");
            if (reason == null) {
                reason = Xml.text(xml, "Reason");
            }
            throw new BridgeException("设备返回 SOAP Fault: " + (reason == null ? "无详细信息" : reason));
        }
        return xml;
    }
}
