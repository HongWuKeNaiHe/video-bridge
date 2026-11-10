package dev.videobridge.onvif;

import dev.videobridge.common.Http;
import dev.videobridge.common.Xml;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 海康私有接口，只用来补 ONVIF 拿不到的通道信息和 NVR 录像段 */
@Component
public class HikvisionIsapi {

    private final OnvifTransport transport;

    public HikvisionIsapi(OnvifTransport transport) {
        this.transport = transport;
    }

    public List<OnvifModels.HikChannel> channels(OnvifDevice device) {
        String xml = transport.get(device, "/ISAPI/ContentMgmt/InputProxy/Channels");
        List<OnvifModels.HikChannel> result = new ArrayList<>();
        for (String block : Xml.blocks(xml, "InputProxyChannel")) {
            String id = Xml.text(block, "id");
            if (id == null) {
                continue;
            }
            result.add(new OnvifModels.HikChannel(id, Xml.text(block, "name"),
                    Xml.text(block, "ipAddress"), Xml.text(block, "port"), Xml.text(block, "status")));
        }
        return result;
    }

    public String deviceName(OnvifDevice device) {
        return Xml.text(transport.get(device, "/ISAPI/System/deviceInfo"), "deviceName");
    }

    public List<OnvifModels.RecordSegment> records(OnvifDevice device, String nvrHost, int httpPort,
                                                    int channelNo, String startTime, String endTime) {
        String start = ReplayUriBuilder.standard(startTime);
        String end = ReplayUriBuilder.standard(endTime);
        if (start == null || end == null) {
            throw new IllegalArgumentException("录像查询时间格式应为 yyyy-MM-dd HH:mm:ss");
        }
        // ISAPI 走 HTTP 端口，和 RTSP 的 554 不是一回事
        String url = "http://" + nvrHost + ":" + httpPort
                + "/ISAPI/ContentMgmt/Record/tracks/" + Math.max(1, channelNo) + "01"
                + "?startTime=" + Http.encode(start) + "&endTime=" + Http.encode(end);

        String xml = transport.get(device, url);
        List<OnvifModels.RecordSegment> result = new ArrayList<>();
        for (String block : Xml.blocks(xml, "mediaSegmentDescriptor")) {
            List<String> times = Xml.texts(block, "localTime");
            if (times.isEmpty()) {
                continue;
            }
            long duration = 0;
            String raw = Xml.text(block, "recordDuration");
            if (raw != null) {
                try {
                    duration = Long.parseLong(raw.trim());
                } catch (NumberFormatException ignored) {
                    // 个别固件回的不是数字，按 0 处理
                }
            }
            result.add(new OnvifModels.RecordSegment(times.get(0),
                    times.size() > 1 ? times.get(1) : "", duration, channelNo));
        }
        return result;
    }
}
