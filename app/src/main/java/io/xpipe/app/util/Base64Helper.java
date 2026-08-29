package io.xpipe.app.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64Helper {

    public static String toBase64Url(String s) {
        var enc = Base64.getUrlEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
        return enc;
    }

    public static String toBase64Url(byte[] b) {
        var enc = Base64.getUrlEncoder().encodeToString(b);
        return enc;
    }

    public static byte[] fromBase64UrlBytes(byte[] b) {
        return Base64.getUrlDecoder().decode(b);
    }

    public static byte[] fromBase64UrlString(String s) {
        return Base64.getUrlDecoder().decode(s.getBytes(StandardCharsets.UTF_8));
    }
}
