package io.xpipe.ext.base.service;

import java.util.HashMap;
import java.util.Map;

public class ServiceAddressRotation {

    private static final Map<String, String> replacedUrls = new HashMap<>();
    private static int counter = 0;
    private static final String[] aliases = new String[]{ "localhost", "127.0.0.1" };

    static String getRotatedLocalhost(String url) {
        if (!url.startsWith("localhost")) {
            return url;
        }

        if (replacedUrls.containsKey(url)) {
            return replacedUrls.get(url);
        }

        var alias = aliases[counter++ % aliases.length];
        var replaced = url.replaceFirst("localhost", alias);
        replacedUrls.put(url, replaced);
        return replaced;
    }
}
