package dev.videobridge;

import dev.videobridge.onvif.Soap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SoapTest {

    @Test
    void presetNameIsEscaped() {
        String xml = Soap.setPreset("Profile001", null, "大门 & <测试> \"1\"");
        assertTrue(xml.contains("大门 &amp; &lt;测试&gt; &quot;1&quot;"));
        assertFalse(xml.contains("<测试>"));
    }

    @Test
    void envelopesCarryTheRightNamespaces() {
        assertTrue(Soap.getProfiles().contains("xmlns:trt=\"http://www.onvif.org/ver10/media/wsdl\""));
        assertTrue(Soap.continuousMove("P1", 0.5f, 0f, 0f)
                .contains("xmlns:tt=\"http://www.onvif.org/ver10/schema\""));
        assertTrue(Soap.getReplayUri("R1", "2026-06-16T00:00:00Z", "2026-06-16T01:00:00Z")
                .contains("xmlns:trp=\"http://www.onvif.org/ver10/replay/wsdl\""));
    }

    @Test
    void continuousMoveUsesSchemaTypesForVelocity() {
        String xml = Soap.continuousMove("P1", -0.5f, 0f, 0.25f);
        assertTrue(xml.contains("<tt:PanTilt x=\"-0.5000\" y=\"0.0000\"/>"));
        assertTrue(xml.contains("<tt:Zoom x=\"0.2500\"/>"));
        assertTrue(xml.contains("<ptz:ProfileToken>P1</ptz:ProfileToken>"));
    }

    @Test
    void setPresetWithoutTokenLetsCameraAssignOne() {
        assertFalse(Soap.setPreset("P1", null, "门口").contains("PresetToken"));
        assertTrue(Soap.setPreset("P1", "7", "门口").contains("<ptz:PresetToken>7</ptz:PresetToken>"));
    }
}
