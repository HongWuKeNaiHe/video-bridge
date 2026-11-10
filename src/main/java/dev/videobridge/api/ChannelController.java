package dev.videobridge.api;

import dev.videobridge.common.ApiResponse;
import dev.videobridge.common.BridgeException;
import dev.videobridge.storage.Channel;
import dev.videobridge.storage.ChannelRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    private final ChannelRepository repository;

    public ChannelController(ChannelRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<List<Channel>> list() {
        return ApiResponse.ok(repository.findAll());
    }

    @GetMapping("/{key}")
    public ApiResponse<Channel> get(@PathVariable String key) {
        return ApiResponse.ok(repository.findByKey(key)
                .orElseThrow(() -> new BridgeException("通道不存在: " + key)));
    }

    @PostMapping
    public ApiResponse<Channel> save(@Valid @RequestBody ApiDtos.ChannelRequest request) {
        return ApiResponse.ok("保存成功", repository.save(request.toChannel()));
    }

    @DeleteMapping("/{key}")
    public ApiResponse<Void> delete(@PathVariable String key) {
        repository.delete(key);
        return ApiResponse.ok("删除成功", null);
    }
}
