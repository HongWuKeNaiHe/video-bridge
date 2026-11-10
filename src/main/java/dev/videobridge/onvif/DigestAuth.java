package dev.videobridge.onvif;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DigestAuth {

    private static final Pattern PARAM = Pattern.compile("([\\w-]+)\\s*=\\s*(?:\"([^\"]*)\"|([^,\\s]*))");

    private DigestAuth() {
    }

    public static Map<String, String> parse(String challenge) {
        Map<String, String> params = new HashMap<>();
        if (challenge == null) {
            return params;
        }
        Matcher m = PARAM.matcher(challenge);
        while (m.find()) {
            String value = m.group(2) != null ? m.group(2) : m.group(3);
            params.put(m.group(1).toLowerCase(Locale.ROOT), value == null ? "" : value);
        }
        return params;
    }

    public static boolean isDigest(String challenge) {
        return challenge != null && challenge.toLowerCase(Locale.ROOT).startsWith("digest");
    }

    public static String basic(String username, String password) {
        String pair = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
    }

    public static String header(String challenge, String username, String password,
                                String method, String uri, String cnonce) {
        Map<String, String> p = parse(challenge);
        String realm = p.getOrDefault("realm", "");
        String nonce = p.getOrDefault("nonce", "");
        String opaque = p.get("opaque");
        String algorithm = p.getOrDefault("algorithm", "MD5");
        // 设备偶尔回 qop="auth,auth-int"，只取能算的那种
        String qop = p.getOrDefault("qop", "auth").split(",")[0].trim();
        String nc = "00000001";

        String ha1 = md5(username + ":" + realm + ":" + password);
        String ha2 = md5(method + ":" + uri);
        String response = md5(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + ha2);

        StringBuilder sb = new StringBuilder("Digest ");
        sb.append("username=\"").append(username).append('"');
        sb.append(", realm=\"").append(realm).append('"');
        sb.append(", nonce=\"").append(nonce).append('"');
        sb.append(", uri=\"").append(uri).append('"');
        sb.append(", algorithm=").append(algorithm);
        sb.append(", qop=").append(qop);
        sb.append(", nc=").append(nc);
        sb.append(", cnonce=\"").append(cnonce).append('"');
        sb.append(", response=\"").append(response).append('"');
        if (opaque != null && !opaque.isEmpty()) {
            sb.append(", opaque=\"").append(opaque).append('"');
        }
        return sb.toString();
    }

    public static String cnonce() {
        byte[] bytes = new byte[8];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK 缺少 MD5", e);
        }
    }
}
