package dev.videobridge.api;

import com.fasterxml.jackson.databind.JsonNode;
import dev.videobridge.common.ApiResponse;
import dev.videobridge.core.ControlService;
import dev.videobridge.core.Preset;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/control")
public class ControlController {

    private final ControlService controlService;

    public ControlController(ControlService controlService) {
        this.controlService = controlService;
    }

    @PostMapping("/ptz")
    public ApiResponse<Void> ptz(@Valid @RequestBody ApiDtos.PtzRequest request) {
        float speed = request.speed() == null ? 0.5f : request.speed();
        controlService.ptz(request.channelKey(), request.command(), speed);
        return ApiResponse.ok("云台指令已下发", null);
    }

    @GetMapping("/presets")
    public ApiResponse<List<Preset>> presets(@RequestParam String channelKey) {
        return ApiResponse.ok(controlService.presets(channelKey));
    }

    @PostMapping("/preset")
    public ApiResponse<Map<String, String>> preset(@Valid @RequestBody ApiDtos.PresetRequest request) {
        String token = controlService.preset(request.channelKey(), request.command(),
                request.presetToken(), request.name());
        return ApiResponse.ok("预置位操作成功", Map.of("presetToken", token));
    }

    @PostMapping("/home-position")
    public ApiResponse<JsonNode> homePosition(@RequestParam String channelKey,
                                              @RequestParam(required = false) Boolean enabled,
                                              @RequestParam(required = false) Integer resetTime,
                                              @RequestParam(required = false) Integer presetIndex) {
        return ApiResponse.ok(controlService.homePosition(channelKey, enabled, resetTime, presetIndex));
    }
}
