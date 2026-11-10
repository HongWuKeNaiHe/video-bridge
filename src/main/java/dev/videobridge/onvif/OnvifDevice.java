package dev.videobridge.onvif;

public record OnvifDevice(String host, int port, String username, String password) {

    public OnvifDevice {
        if (port <= 0) {
            port = 80;
        }
    }

    public String baseUrl() {
        return "http://" + host + (port == 80 ? "" : ":" + port);
    }
}
