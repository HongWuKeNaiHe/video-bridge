package dev.videobridge.api;

import com.fasterxml.jackson.databind.JsonNode;
import dev.videobridge.common.ApiResponse;
import dev.videobridge.core.PlaybackService;
import dev.videobridge.core.StreamTicket;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/playback")
public class PlaybackController {

    private final PlaybackService playbackService;

    public PlaybackController(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    @PostMapping("/start")
    public ApiResponse<StreamTicket> start(@Valid @RequestBody ApiDtos.PlaybackRequest request) {
        boolean download = request.download() != null && request.download();
        return ApiResponse.ok(download ? "开始下载成功" : "开始回放成功",
                playbackService.start(request.channelKey(), request.startTime(), request.endTime(), download));
    }

    @PostMapping("/stop")
    public ApiResponse<Void> stop(@RequestParam String streamKey) {
        playbackService.stop(streamKey);
        return ApiResponse.ok("已停止", null);
    }

    @GetMapping("/progress")
    public ApiResponse<Map<String, Object>> progress(@RequestParam String streamKey) {
        return ApiResponse.ok(playbackService.progress(streamKey));
    }

    @PostMapping("/control")
    public ApiResponse<JsonNode> control(@Valid @RequestBody ApiDtos.ControlRequest request) {
        return ApiResponse.ok(playbackService.control(request.streamKey(), request.type(),
                request.seek(), request.speed()));
    }

    @GetMapping("/gb-records")
    public ApiResponse<JsonNode> gbRecords(@RequestParam String channelKey,
                                           @RequestParam String startTime,
                                           @RequestParam String endTime) {
        return ApiResponse.ok(playbackService.gbRecords(channelKey, startTime, endTime));
    }
}
