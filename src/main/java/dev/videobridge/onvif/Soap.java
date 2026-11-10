package dev.videobridge.onvif;

import dev.videobridge.common.Xml;

import java.util.Locale;

public final class Soap {

    private static final String DEVICE_NS = " xmlns:tds=\"" + OnvifPaths.NS_DEVICE + "\"";
    private static final String MEDIA_NS = " xmlns:trt=\"" + OnvifPaths.NS_MEDIA + "\" xmlns:tt=\"" + OnvifPaths.NS_SCHEMA + "\"";
    private static final String PTZ_NS = " xmlns:ptz=\"" + OnvifPaths.NS_PTZ + "\" xmlns:tt=\"" + OnvifPaths.NS_SCHEMA + "\"";
    private static final String REPLAY_NS = " xmlns:trp=\"" + OnvifPaths.NS_REPLAY + "\" xmlns:tt=\"" + OnvifPaths.NS_SCHEMA + "\"";

    private Soap() {
    }

    public static String deviceInfo() {
        return envelope(DEVICE_NS, "<tds:GetDeviceInformation/>");
    }

    public static String getScopes() {
        return envelope(DEVICE_NS, "<tds:GetScopes/>");
    }

    public static String getHostname() {
        return envelope(DEVICE_NS, "<tds:GetHostname/>");
    }

    public static String getServices() {
        return envelope(DEVICE_NS, "<tds:GetServices><tds:IncludeCapability>false</tds:IncludeCapability></tds:GetServices>");
    }

    public static String getProfiles() {
        return envelope(MEDIA_NS, "<trt:GetProfiles/>");
    }

    public static String getVideoSources() {
        return envelope(MEDIA_NS, "<trt:GetVideoSources/>");
    }

    public static String getStreamUri(String profileToken) {
        return envelope(MEDIA_NS, "<trt:GetStreamUri>"
                + "<trt:StreamSetup><tt:Stream>RTP-Unicast</tt:Stream>"
                + "<tt:Transport><tt:Protocol>RTSP</tt:Protocol></tt:Transport></trt:StreamSetup>"
                + "<trt:ProfileToken>" + Xml.escape(profileToken) + "</trt:ProfileToken>"
                + "</trt:GetStreamUri>");
    }

    public static String getReplayUri(String recordingToken, String startIso, String endIso) {
        return envelope(REPLAY_NS, "<trp:GetReplayUri>"
                + "<trp:StreamSetup><tt:Stream>RTP-Unicast</tt:Stream>"
                + "<tt:Transport><tt:Protocol>RTSP</tt:Protocol></tt:Transport></trp:StreamSetup>"
                + "<trp:RecordingToken>" + Xml.escape(recordingToken) + "</trp:RecordingToken>"
                + "<trp:StartPoint><tt:UtcTime>" + startIso + "</tt:UtcTime></trp:StartPoint>"
                + "<trp:EndPoint><tt:UtcTime>" + endIso + "</tt:UtcTime></trp:EndPoint>"
                + "</trp:GetReplayUri>");
    }

    public static String continuousMove(String profileToken, float pan, float tilt, float zoom) {
        return envelope(PTZ_NS, "<ptz:ContinuousMove>"
                + "<ptz:ProfileToken>" + Xml.escape(profileToken) + "</ptz:ProfileToken>"
                + "<ptz:Velocity>"
                + "<tt:PanTilt x=\"" + fmt(pan) + "\" y=\"" + fmt(tilt) + "\"/>"
                + "<tt:Zoom x=\"" + fmt(zoom) + "\"/>"
                + "</ptz:Velocity></ptz:ContinuousMove>");
    }

    public static String stop(String profileToken) {
        return envelope(PTZ_NS, "<ptz:Stop>"
                + "<ptz:ProfileToken>" + Xml.escape(profileToken) + "</ptz:ProfileToken>"
                + "<tt:PanTilt>true</tt:PanTilt><tt:Zoom>true</tt:Zoom></ptz:Stop>");
    }

    public static String getPresets(String profileToken) {
        return envelope(PTZ_NS, "<ptz:GetPresets><ptz:ProfileToken>"
                + Xml.escape(profileToken) + "</ptz:ProfileToken></ptz:GetPresets>");
    }

    public static String setPreset(String profileToken, String presetToken, String name) {
        StringBuilder body = new StringBuilder("<ptz:SetPreset><ptz:ProfileToken>")
                .append(Xml.escape(profileToken)).append("</ptz:ProfileToken>");
        if (presetToken != null && !presetToken.isEmpty()) {
            body.append("<ptz:PresetToken>").append(Xml.escape(presetToken)).append("</ptz:PresetToken>");
        }
        if (name != null && !name.isEmpty()) {
            body.append("<ptz:PresetName>").append(Xml.escape(name)).append("</ptz:PresetName>");
        }
        return envelope(PTZ_NS, body.append("</ptz:SetPreset>").toString());
    }

    public static String gotoPreset(String profileToken, String presetToken) {
        return envelope(PTZ_NS, "<ptz:GotoPreset><ptz:ProfileToken>" + Xml.escape(profileToken)
                + "</ptz:ProfileToken><ptz:PresetToken>" + Xml.escape(presetToken)
                + "</ptz:PresetToken></ptz:GotoPreset>");
    }

    public static String removePreset(String profileToken, String presetToken) {
        return envelope(PTZ_NS, "<ptz:RemovePreset><ptz:ProfileToken>" + Xml.escape(profileToken)
                + "</ptz:ProfileToken><ptz:PresetToken>" + Xml.escape(presetToken)
                + "</ptz:PresetToken></ptz:RemovePreset>");
    }

    private static String envelope(String namespaces, String body) {
        return "<soap:Envelope xmlns:soap=\"" + OnvifPaths.NS_SOAP + "\"" + namespaces
                + "><soap:Body>" + body + "</soap:Body></soap:Envelope>";
    }

    private static String fmt(float value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
