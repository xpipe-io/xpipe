package io.xpipe.ext.base.service;

import java.util.HashMap;
import java.util.Map;

public class ServiceAddressRotation {

    private static final Map<String, String> replacedUrls = new HashMap<>();
    private static int counter = 0;
    private static final String[] aliases = new String[]{ "localhost", "127.0.0.1" };

    private static String getRotatedLocalhost(String url) {
        if (!url.startsWith("localhost")) {
            return url;
        }

        if (replacedUrls.containsKey(url)) {
            return replacedUrls.get(url);
        }

        var alias = aliases[counter++ % aliases.length];
        var replaced = url.replace("localhost", alias);
        replacedUrls.put(url, replaced);
        return replaced;
    }

    public static String getRotatedAddress(AbstractServiceStore serviceStore) {
        var s = serviceStore.getSession();
        if (s == null) {
            var host = serviceStore.getHost().getStore().getTunnelHostName() != null
                    ? serviceStore.getHost().getStore().getTunnelHostName()
                    : "localhost";
            return getRotatedLocalhost(host + ":" + serviceStore.getRemotePort());
        }

        return getRotatedLocalhost("localhost:" + s.getLocalPort());
    }
}
