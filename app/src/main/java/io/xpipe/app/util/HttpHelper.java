package io.xpipe.app.util;

import io.xpipe.app.prefs.AppPrefs;

import lombok.SneakyThrows;

import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpHelper {

    @SneakyThrows
    public static HttpClient client() {
        var builder = HttpClient.newBuilder();
        builder.followRedirects(HttpClient.Redirect.NORMAL);
        if (AppPrefs.get() != null && AppPrefs.get().disableApiAuthentication().get()) {
            var sslContext = SSLContext.getInstance("TLS");
            var trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[] {};
                }
            };
            sslContext.init(null, new TrustManager[] {trustManager}, new SecureRandom());
            builder.sslContext(sslContext);
        }
        return builder.build();
    }
}
