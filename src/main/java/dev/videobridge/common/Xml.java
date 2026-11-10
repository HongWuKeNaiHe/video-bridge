package dev.videobridge.common;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ONVIF 和 ISAPI 返回的 XML 前缀各家不一样，用 DOM 按命名空间查反而容易漏，
 * 这里统一按「忽略前缀的标签名」取内容。
 */
public final class Xml {

    private static final String NS = "(?:[A-Za-z_][\\w.-]*:)?";

    private Xml() {
    }

    /** 取第一个同名标签的文本，取不到返回 null */
    public static String text(String xml, String tag) {
        List<String> values = texts(xml, tag);
        return values.isEmpty() ? null : values.get(0);
    }

    public static List<String> texts(String xml, String tag) {
        List<String> values = new ArrayList<>();
        if (xml == null) {
            return values;
        }
        Matcher m = element(tag).matcher(xml);
        while (m.find()) {
            values.add(decode(m.group(1).trim()));
        }
        return values;
    }

    /** 取完整节点（含开始标签），这样属性和子节点都能继续解析 */
    public static List<String> blocks(String xml, String tag) {
        List<String> blocks = new ArrayList<>();
        if (xml == null) {
            return blocks;
        }
        Matcher m = element(tag).matcher(xml);
        while (m.find()) {
            blocks.add(m.group(0));
        }
        return blocks;
    }

    /** 取节点开始标签上的属性，如 <Profiles token="..."> */
    public static String attr(String block, String tag, String name) {
        if (block == null) {
            return null;
        }
        Pattern p = Pattern.compile("<" + NS + Pattern.quote(tag) + "\\b[^>]*?\\s" + Pattern.quote(name)
                + "\\s*=\\s*\"([^\"]*)\"", Pattern.DOTALL);
        Matcher m = p.matcher(block);
        return m.find() ? decode(m.group(1)) : null;
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static Pattern element(String tag) {
        String quoted = Pattern.quote(tag);
        return Pattern.compile("<" + NS + quoted + "\\b[^>]*>(.*?)</" + NS + quoted + "\\s*>",
                Pattern.DOTALL);
    }

    private static String decode(String value) {
        return value.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }
}
