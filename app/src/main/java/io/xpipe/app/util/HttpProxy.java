package io.xpipe.app.util;

import io.xpipe.app.core.AppCertStore;
import io.xpipe.app.ext.ProcModuleProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.secret.InPlaceSecretValue;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.store.DataStore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class HttpProxy {

    public static Optional<HttpProxy> detectFromEnvironment() {
        var envOrder = List.of("HTTP_PROXY", "http_proxy", "HTTPS_PROXY", "https_proxy", "ALL_PROXY", "all_proxy");
        for (String s : envOrder) {
            var env = System.getenv(s);
            if (env != null) {
                try {
                    var parsed = URI.create(env);
                    var isSocks = parsed.getScheme() != null && parsed.getScheme().equals("socks5");
                    var host = parsed.getHost();
                    var port = parsed.getPort() != -1 ? parsed.getPort() : isSocks ? 1080 : 8080;
                    var userInfo = parsed.getRawUserInfo();
                    var separatorIndex = userInfo != null ? userInfo.indexOf(':') : -1;
                    var user = userInfo != null
                            ? decodeUserInfo(separatorIndex != -1 ? userInfo.substring(0, separatorIndex) : userInfo)
                            : null;
                    if (user != null && user.isBlank()) {
                        user = null;
                    }
                    var pass = separatorIndex != -1 ? decodeUserInfo(userInfo.substring(separatorIndex + 1)) : null;
                    if (pass != null && pass.isBlank()) {
                        pass = null;
                    }
                    return Optional.of(new HttpProxy(
                            host, port, user, pass != null ? InPlaceSecretValue.of(pass) : null, isSocks));
                } catch (IllegalArgumentException ignored) {
                }
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

        AppCertStore.getBundleFile().ifPresent(bundleFile -> {
            map.put("ssl_cert_file", bundleFile.toString());
            map.put("SSL_CERT_FILE", bundleFile.toString());
        });

        var np = AppPrefs.get().noProxyList().getValue();
        if (np != null && !np.isEmpty()) {
            var val = np.lines().collect(Collectors.joining(","));
            map.put("no_proxy", val);
            map.put("NO_PROXY", val);
        }

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
            return ProcModuleProvider.get().getHttpProxy(ref).isPresent();
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return false;
        }
    }

    private static String encodeUserInfo(String s) {
        // URLEncoder and URLDecoder are meant for form data which treats + as a space
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String decodeUserInfo(String s) {
        // URLEncoder and URLDecoder are meant for form data which treats + as a space
        return URLDecoder.decode(s.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    public String toUrl() {
        var userInfo = "";
        if (user != null && !user.isBlank()) {
            userInfo = encodeUserInfo(user);
            if (password != null && !password.getSecretValue().isBlank()) {
                userInfo += ":" + encodeUserInfo(password.getSecretValue());
            }
            userInfo += "@";
        }

        var scheme = socks5 ? "socks5" : "http";
        return scheme + "://" + userInfo + host + ":" + port;
    }

    String host;
    int port;
    String user;
    InPlaceSecretValue password;
    boolean socks5;
}
