CREATE TABLE IF NOT EXISTS vb_channel
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_key     VARCHAR(128) NOT NULL UNIQUE,
    name            VARCHAR(255),
    protocol        VARCHAR(16)  NOT NULL,
    gb_device_id    VARCHAR(64),
    gb_channel_id   VARCHAR(64),
    onvif_host      VARCHAR(255),
    onvif_port      INT,
    onvif_username  VARCHAR(128),
    onvif_password  VARCHAR(256),
    nvr_host        VARCHAR(255),
    channel_no      INT,
    vendor          VARCHAR(32) DEFAULT 'AUTO',
    ptz_enabled     BOOLEAN     DEFAULT TRUE,
    permanent       BOOLEAN     DEFAULT FALSE,
    enabled         BOOLEAN     DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
