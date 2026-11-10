package dev.videobridge.onvif;

public final class OnvifPaths {

    // 常见 NVR 把四类服务拆在不同路径上；标准做法是先 GetServices 取 XAddr，
    // 但现场设备基本都认下面这几个，少一次往返，遇到不认的再扩展发现逻辑。
    public static final String DEVICE = "/onvif/device_service";
    public static final String MEDIA = "/onvif/media_service";
    public static final String PTZ = "/onvif/ptzx";
    public static final String REPLAY = "/onvif/replay";

    public static final String NS_DEVICE = "http://www.onvif.org/ver10/device/wsdl";
    public static final String NS_MEDIA = "http://www.onvif.org/ver10/media/wsdl";
    public static final String NS_PTZ = "http://www.onvif.org/ver20/ptz/wsdl";
    public static final String NS_REPLAY = "http://www.onvif.org/ver10/replay/wsdl";
    public static final String NS_SCHEMA = "http://www.onvif.org/ver10/schema";
    public static final String NS_SOAP = "http://www.w3.org/2003/05/soap-envelope";

    private OnvifPaths() {
    }
}
