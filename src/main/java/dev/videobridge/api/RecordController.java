package dev.videobridge.api;

import dev.videobridge.common.ApiResponse;
import dev.videobridge.core.RecordService;
import dev.videobridge.onvif.OnvifModels;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/record")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping("/start")
    public ApiResponse<Map<String, Object>> start(@RequestParam String channelKey) {
        return ApiResponse.ok("开始录像成功", recordService.start(channelKey));
    }

    @PostMapping("/stop")
    public ApiResponse<Map<String, Object>> stop(@RequestParam String channelKey) {
        return ApiResponse.ok(recordService.stop(channelKey));
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(@RequestParam String channelKey) {
        return ApiResponse.ok(recordService.status(channelKey));
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(@RequestParam String channelKey,
                                                 @RequestParam String startTime,
                                                 @RequestParam String endTime) {
        return ApiResponse.ok(recordService.list(channelKey, startTime, endTime));
    }

    @GetMapping("/nvr")
    public ApiResponse<List<OnvifModels.RecordSegment>> nvr(@RequestParam String channelKey,
                                                            @RequestParam String startTime,
                                                            @RequestParam String endTime) {
        return ApiResponse.ok(recordService.nvrRecords(channelKey, startTime, endTime));
    }
}
