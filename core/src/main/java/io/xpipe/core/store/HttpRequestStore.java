package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

@Value
@JsonTypeName("httpRequest")
public class HttpRequestStore implements StreamDataStore {

    public static boolean isHttpRequest(String s) {
        return s.startsWith("http:") || s.startsWith("https:");
    }

    public static Optional<HttpRequestStore> fromString(String s) {
        try {
            var uri = new URI(s);
            return Optional.of(new HttpRequestStore(uri, Map.of()));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    URI uri;
    Map<String, String> headers;

    @Override
    public InputStream openInput() throws Exception {
        var b = HttpRequest.newBuilder().uri(uri);
        headers.forEach(b::setHeader);
        var req = b.GET().build();

        var client = HttpClient.newHttpClient();
        var res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());

        return new ByteArrayInputStream(res.body());
    }

    @Override
    public boolean exists() {
        return false;
    }
}
