package io.xpipe.app.util;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStoreEntryRef;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class HttpProxy {

    public static Optional<HttpProxy> detectFromEnvironment() {
        var envOrder = List.of("HTTP_PROXY", "http_proxy", "HTTPS_PROXY", "https_proxy", "ALL_PROXY", "all_proxy");
        for (String s : envOrder) {
            var env =  System.getenv(s);
            if (env != null) {
                try {
                    var parsed = URI.create(env);
                    var isSocks = parsed.getScheme().equals("socks5");
                    var host = parsed.getHost();
                    var port = parsed.getPort() != -1 ? parsed.getPort() : isSocks ? 1080 : 8080;
                    var userInfo = parsed.getUserInfo();
                    var user = userInfo != null ? userInfo.split(":")[0] : null;
                    var pass = userInfo != null && userInfo.contains(":") ? userInfo.split(":")[1] : null;
                    return Optional.of(new HttpProxy(host, port, user, pass != null ? InPlaceSecretValue.of(pass) : null, isSocks));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        return Optional.empty();
    }

    public static Map<String, String> getEnvironmentVariables() {
        var proxy = getActiveProxy();
        if (proxy.isEmpty()) {
            return Map.of();
        }

        var map = new LinkedHashMap<String, String>();
        var http = proxy.get().toUrl();
        map.put("http_proxy", http);
        map.put("HTTP_PROXY", http);

        // Use HTTP protocol as well here as most proxies still require that
        map.put("https_proxy", http);
        map.put("HTTPS_PROXY", http);
        return map;
    }

    public static Optional<HttpProxy> getActiveProxy() {
        if (AppPrefs.get() == null) {
            return Optional.empty();
        }

        var current = AppPrefs.get().httpProxy().getValue();
        if (current == null) {
            return Optional.empty();
        }

        return Optional.of(current);
    }

    public static boolean disableTlsVerification() {
        return AppPrefs.get() != null && AppPrefs.get().disableHttpsTlsCheck().getValue();
    }

    public static boolean canUseAsProxy(DataStoreEntryRef<DataStore> ref) {
        if (!ref.get().getValidity().isUsable()) {
            return false;
        }

        try {
            return ProcessControlProvider.get().getHttpProxy(ref).isPresent();
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return false;
        }
    }

    public String toUrl() {
        return (socks5 ? "socks5" : "http") + "://"
                + (user != null && password != null ? user + ":" + password.getSecretValue() + "@" : "") + host + ":"
                + port;
    }

    public boolean hasAuth() {
        return user != null && password != null;
    }

    String host;
    int port;
    String user;
    InPlaceSecretValue password;
    boolean socks5;
}
