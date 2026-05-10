package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorAction;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;

import io.xpipe.app.core.AppCertStore;
import lombok.SneakyThrows;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import javax.net.ssl.*;

public class HttpHelper {

    @SneakyThrows
    public static HttpClient client() {
        var proxy = HttpProxy.getActiveProxy();
        return client(
                proxy.orElse(null),
                HttpProxy.disableTlsVerification());
    }

    @SneakyThrows
    public static HttpClient client(HttpProxy proxy, boolean checkTls) {
        var builder = HttpClient.newBuilder();
        builder.connectTimeout(Duration.ofSeconds(5));
        builder.version(HttpClient.Version.HTTP_1_1);
        builder.followRedirects(HttpClient.Redirect.NORMAL);

        if (!checkTls) {
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
        } else {
            var certStore = AppCertStore.get();
            if (certStore != null) {
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, new TrustManager[]{certStore.getCustomTrustManager()}, null);
                builder.sslContext(context);
            }
        }

        if (proxy != null) {
            builder.proxy(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    return List.of(new Proxy(
                            proxy.isSocks5() ? Proxy.Type.SOCKS : Proxy.Type.HTTP,
                            new InetSocketAddress(proxy.getHost(), proxy.getPort())));
                }

                @Override
                public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                    ErrorEventFactory.fromThrowable(ioe).handle();
                }
            });
            builder.authenticator(new Authenticator() {

                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    if (proxy.getUser() != null && proxy.getPassword() != null) {
                        return new PasswordAuthentication(
                                proxy.getUser(), proxy.getPassword().getSecret());
                    } else {
                        return null;
                    }
                }
            });
        }

        return builder.build();
    }

    public static void checkOrThrow(HttpResponse<?> res) throws IOException {
        if (res.statusCode() == 407) {
            var ex = new IOException("HTTP proxy authentication required");
            ErrorEventFactory.preconfigure(
                    ErrorEventFactory.fromThrowable(ex).expected().customAction(new ErrorAction() {
                        @Override
                        public String getName() {
                            return AppI18n.get("httpProxyError");
                        }

                        @Override
                        public String getDescription() {
                            return AppI18n.get("httpProxyErrorDescription");
                        }

                        @Override
                        public boolean handle(ErrorEvent event) {
                            AppPrefs.get().selectCategory("httpProxy");
                            return true;
                        }
                    }));
            throw ex;
        }

        if (res.statusCode() >= 400) {
            if (res.body() instanceof String s) {
                var msg = !s.isEmpty() ? s : "Received HTTP " + res.statusCode() + " without further details";
                throw new IOException(msg);
            } else if (res.body() instanceof byte[] b) {
                var msg = b.length > 0
                        ? new String(b, StandardCharsets.UTF_8)
                        : "Received HTTP " + res.statusCode() + " without further details";
                throw new IOException(msg);
            }
        }
    }
}
