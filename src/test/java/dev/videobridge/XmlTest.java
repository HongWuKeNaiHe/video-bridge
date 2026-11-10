package dev.videobridge;

import dev.videobridge.common.Xml;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XmlTest {

    private static final String PROFILES = "<trt:GetProfilesResponse>"
            + "<trt:Profiles token=\"Profile_1\" fixed=\"true\"><tt:Name>主码流</tt:Name>"
            + "<tt:VideoSourceConfiguration><tt:SourceToken>VS_1</tt:SourceToken>"
            + "</tt:VideoSourceConfiguration></trt:Profiles>"
            + "<trt:Profiles token=\"Profile_2\"><tt:Name>子码流</tt:Name></trt:Profiles>"
            + "</trt:GetProfilesResponse>";

    @Test
    void blocksIgnoreNamespacePrefixes() {
        List<String> blocks = Xml.blocks(PROFILES, "Profiles");
        assertEquals(2, blocks.size());
        assertEquals("Profile_1", Xml.attr(blocks.get(0), "Profiles", "token"));
        assertEquals("主码流", Xml.text(blocks.get(0), "Name"));
        assertEquals("VS_1", Xml.text(blocks.get(0), "SourceToken"));
        assertEquals("Profile_2", Xml.attr(blocks.get(1), "Profiles", "token"));
    }

    @Test
    void textDecodesEntities() {
        assertEquals("A & B <C>", Xml.text("<tt:Name>A &amp; B &lt;C&gt;</tt:Name>", "Name"));
        assertNull(Xml.text("<tt:Name>nothing</tt:Name>", "Uri"));
    }

    @Test
    void textsKeepsAllOccurrences() {
        List<String> times = Xml.texts("<a><localTime>2026-06-16T00:00:00</localTime>"
                + "<b><localTime>2026-06-16T01:00:00</localTime></b></a>", "localTime");
        assertEquals(List.of("2026-06-16T00:00:00", "2026-06-16T01:00:00"), times);
    }

    @Test
    void escapeCoversAllXmlSpecials() {
        assertEquals("&amp;&lt;&gt;&quot;&apos;", Xml.escape("&<>\"'"));
        assertEquals("", Xml.escape(null));
    }
}
