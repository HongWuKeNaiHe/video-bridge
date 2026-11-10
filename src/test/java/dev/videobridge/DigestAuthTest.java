package dev.videobridge;

import dev.videobridge.onvif.DigestAuth;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DigestAuthTest {

    @Test
    void rfc2617Example() {
        String challenge = "Digest realm=\"testrealm@host.com\", qop=\"auth,auth-int\", "
                + "nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\", "
                + "opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"";
        String header = DigestAuth.header(challenge, "Mufasa", "Circle Of Life",
                "GET", "/dir/index.html", "0a4f113b");

        assertTrue(header.contains("response=\"6629fae49393a05397450978507c4ef1\""));
        assertTrue(header.contains("qop=auth"));
        assertTrue(header.contains("opaque=\"5ccc069c403ebaf9f0171e9517f40e41\""));
    }

    @Test
    void parsesQuotedValuesAndCommaInsideQop() {
        Map<String, String> params = DigestAuth.parse(
                "Digest realm=\"IP Camera\", nonce=\"abc123\", qop=\"auth,auth-int\", algorithm=MD5");
        assertEquals("IP Camera", params.get("realm"));
        assertEquals("abc123", params.get("nonce"));
        assertEquals("auth,auth-int", params.get("qop"));
        assertEquals("MD5", params.get("algorithm"));
    }

    @Test
    void basicHeaderIsBase64() {
        assertEquals("Basic YWRtaW46MTIzNDU2", DigestAuth.basic("admin", "123456"));
    }

    @Test
    void md5OfEmptyString() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", DigestAuth.md5(""));
    }
}
