package io.xpipe.ext.base;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.util.Validators;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.store.DataFlow;
import io.xpipe.core.store.StatefulDataStore;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.core.util.JacksonizedValue;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Arrays;
import java.util.Map;

@JsonTypeName("http")
@SuperBuilder
@Jacksonized
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
public class HttpStore extends JacksonizedValue implements StreamDataStore, StatefulDataStore {

    String method;
    String uriString;
    Map<String, String> headers;
    DataFlow flow;

    @Value
    @Jacksonized
    @SuperBuilder
    public static class CachedResponseInfo {

        public static CachedResponseInfo parse(HttpResponse<?> res) {
            var builder = CachedResponseInfo.builder();
            builder.contentEncoding(res.headers().firstValue("Content-Encoding").orElse(null));
            var contentType = res.headers().firstValue("Content-Type");
            if (contentType.isPresent()) {
                var s = Arrays.stream(contentType.get().split(";"))
                        .map(String::trim)
                        .toList();
                builder.mimeType(s.get(0));
                if (s.size() > 1) {
                    try {
                        var cs = Charset.forName(s.get(1));
                        builder.charset(StreamCharset.get(cs, false));
                    } catch (IllegalCharsetNameException ignored) {

                    }
                }
            }
            return builder.build();
        }

        String contentEncoding;
        StreamCharset charset;
        String mimeType;
    }

    public CachedResponseInfo getInfo() {
        return getState(
                "info", CachedResponseInfo.class, CachedResponseInfo.builder().build());
    }

    public URI getURL() {
        try {
            return uriString != null ? URI.create(uriString) : null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DataFlow getFlow() {
        return flow;
    }

    @Override
    public boolean canOpen() {
        return flow == DataFlow.INPUT;
    }

    @Override
    public void checkComplete() throws Exception {
        Validators.nonNull(method, "Method");
        Validators.nonNull(uriString, "URL");
        URI.create(uriString);
        Validators.nonNull(headers, "Headers");
    }

    @Override
    public void validate() throws Exception {
        var client = createClient();
        var request = createRequest();
        var response = client.send(request, HttpResponse.BodyHandlers.discarding());
        setState("info", CachedResponseInfo.parse(response));
    }

    private HttpClient createClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    private HttpRequest createRequest() {
        var b = HttpRequest.newBuilder().uri(getURL());
        headers.forEach(b::setHeader);
        return b.GET().build();
    }

    @Override
    public InputStream openInput() throws Exception {
        var request = createRequest();
        var client = createClient();
        var res = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (res.statusCode() >= 400) {
            throw new IOException("Returned HTTP status code is " + res.statusCode());
        }

        setState("info", CachedResponseInfo.parse(res));

        return res.body();
    }
}
