package dev.videobridge.api;

import dev.videobridge.common.ApiResponse;
import dev.videobridge.core.StreamService;
import dev.videobridge.core.StreamTicket;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stream")
public class StreamController {

    private final StreamService streamService;

    public StreamController(StreamService streamService) {
        this.streamService = streamService;
    }

    @PostMapping("/start")
    public ApiResponse<StreamTicket> start(@RequestParam String channelKey) {
        return ApiResponse.ok("开始直播成功", streamService.start(channelKey));
    }

    @PostMapping("/stop")
    public ApiResponse<Void> stop(@RequestParam String channelKey) {
        streamService.stop(channelKey);
        return ApiResponse.ok("停止直播成功", null);
    }

    @PostMapping("/touch")
    public ApiResponse<Void> touch(@RequestParam String channelKey) {
        streamService.touch(channelKey);
        return ApiResponse.ok("保活完成", null);
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status(@RequestParam String channelKey) {
        return ApiResponse.ok(streamService.status(channelKey));
    }

    @GetMapping(value = "/snap", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> snap(@RequestParam String channelKey) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(streamService.snap(channelKey));
    }
}
