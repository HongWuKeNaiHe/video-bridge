package dev.videobridge.api;

import com.fasterxml.jackson.databind.JsonNode;
import dev.videobridge.common.ApiResponse;
import dev.videobridge.config.BridgeProperties;
import dev.videobridge.core.StreamService;
import dev.videobridge.onvif.OnvifModels;
import dev.videobridge.onvif.OnvifPaths;
import dev.videobridge.wvp.WvpApi;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class DiscoveryController {

    private final StreamService streamService;
    private final BridgeProperties props;

    public DiscoveryController(StreamService streamService, BridgeProperties props) {
        this.streamService = streamService;
        this.props = props;
    }

    @GetMapping("/api/wvp/devices")
    public ApiResponse<JsonNode> devices(@RequestParam(required = false) Integer start,
                                         @RequestParam(required = false) Integer limit,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(required = false) Boolean online) {
        return ApiResponse.ok(streamService.devices(start, limit, q, online));
    }

    @GetMapping("/api/wvp/channels")
    public ApiResponse<JsonNode> channels(@RequestParam(required = false) String serial,
                                          @RequestParam(required = false) String channelType,
                                          @RequestParam(required = false) String code,
                                          @RequestParam(required = false) Integer start,
                                          @RequestParam(required = false) Integer limit,
                                          @RequestParam(required = false) String q,
                                          @RequestParam(required = false) Boolean online) {
        return ApiResponse.ok(streamService.channels(serial, channelType, code, start, limit, q, online));
    }

    @PostMapping("/api/wvp/token/clear")
    public ApiResponse<Void> clearToken() {
        streamService.clearToken();
        return ApiResponse.ok("token 缓存已清除", null);
    }

    /** 不落库，接设备时先探一下能不能通、有哪些 Profile */
    @PostMapping("/api/onvif/probe")
    public ApiResponse<OnvifModels.DiscoveredDevice> probe(@Valid @RequestBody ApiDtos.OnvifProbe request) {
        return ApiResponse.ok(streamService.probe(request.toDevice()));
    }

    @PostMapping("/api/onvif/channels")
    public ApiResponse<List<OnvifModels.HikChannel>> isapiChannels(@Valid @RequestBody ApiDtos.OnvifProbe request) {
        return ApiResponse.ok(streamService.isapiChannels(request.toDevice()));
    }

    @GetMapping("/api/meta")
    public ApiResponse<Map<String, Object>> meta() {
        return ApiResponse.ok(Map.of(
                "protocols", List.of("ONVIF", "GB28181"),
                "onvifPaths", Map.of("device", OnvifPaths.DEVICE, "media", OnvifPaths.MEDIA,
                        "ptz", OnvifPaths.PTZ, "replay", OnvifPaths.REPLAY),
                "wvpEndpoints", WvpApi.defaults(),
                "features", Map.of("forkExtensions", props.getWvp().isForkExtensions(),
                        "shareZlm", props.getWvp().isShareZlm(),
                        "stateType", props.getState().getType())));
    }
}
