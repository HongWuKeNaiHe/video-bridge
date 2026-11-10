package dev.videobridge.storage;

import dev.videobridge.core.VideoProtocol;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ChannelRepository {

    private static final String COLUMNS = "id, channel_key, name, protocol, gb_device_id, gb_channel_id, "
            + "onvif_host, onvif_port, onvif_username, onvif_password, nvr_host, channel_no, "
            + "vendor, ptz_enabled, permanent, enabled, created_at, updated_at";

    private static final RowMapper<Channel> MAPPER = (rs, row) -> new Channel(
            rs.getLong("id"),
            rs.getString("channel_key"),
            rs.getString("name"),
            VideoProtocol.parse(rs.getString("protocol")),
            rs.getString("gb_device_id"),
            rs.getString("gb_channel_id"),
            rs.getString("onvif_host"),
            rs.getObject("onvif_port", Integer.class),
            rs.getString("onvif_username"),
            rs.getString("onvif_password"),
            rs.getString("nvr_host"),
            rs.getObject("channel_no", Integer.class),
            rs.getString("vendor"),
            rs.getBoolean("ptz_enabled"),
            rs.getBoolean("permanent"),
            rs.getBoolean("enabled"),
            time(rs.getTimestamp("created_at")),
            time(rs.getTimestamp("updated_at")));

    private final JdbcTemplate jdbc;

    public ChannelRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Channel> findAll() {
        return jdbc.query("select " + COLUMNS + " from vb_channel order by id", MAPPER);
    }

    public Optional<Channel> findByKey(String channelKey) {
        return jdbc.query("select " + COLUMNS + " from vb_channel where channel_key = ?", MAPPER, channelKey)
                .stream().findFirst();
    }

    public Channel save(Channel c) {
        if (findByKey(c.channelKey()).isPresent()) {
            jdbc.update("update vb_channel set name=?, protocol=?, gb_device_id=?, gb_channel_id=?, "
                            + "onvif_host=?, onvif_port=?, onvif_username=?, onvif_password=?, nvr_host=?, "
                            + "channel_no=?, vendor=?, ptz_enabled=?, permanent=?, enabled=?, "
                            + "updated_at=current_timestamp where channel_key=?",
                    c.name(), c.protocol().name(), c.gbDeviceId(), c.gbChannelId(),
                    c.onvifHost(), c.onvifPort(), c.onvifUsername(), c.onvifPassword(), c.nvrHost(),
                    c.channelNo(), c.vendor(), c.ptzEnabled(), c.permanent(), c.enabled(), c.channelKey());
        } else {
            jdbc.update("insert into vb_channel (channel_key, name, protocol, gb_device_id, gb_channel_id, "
                            + "onvif_host, onvif_port, onvif_username, onvif_password, nvr_host, channel_no, "
                            + "vendor, ptz_enabled, permanent, enabled) "
                            + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    c.channelKey(), c.name(), c.protocol().name(), c.gbDeviceId(), c.gbChannelId(),
                    c.onvifHost(), c.onvifPort(), c.onvifUsername(), c.onvifPassword(), c.nvrHost(),
                    c.channelNo(), c.vendor(), c.ptzEnabled(), c.permanent(), c.enabled());
        }
        return findByKey(c.channelKey()).orElseThrow();
    }

    public void delete(String channelKey) {
        jdbc.update("delete from vb_channel where channel_key = ?", channelKey);
    }

    private static LocalDateTime time(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
