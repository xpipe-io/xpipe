package io.xpipe.ext.base.service;

import io.xpipe.app.util.Hyperlinks;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ServiceProtocolType.None.class),
    @JsonSubTypes.Type(value = ServiceProtocolType.Http.class),
    @JsonSubTypes.Type(value = ServiceProtocolType.Https.class)
})
public interface ServiceProtocolType {

    String formatUrl(String base);

    void open(String url);

    String getTranslationKey();

    @JsonTypeName("none")
    @Value
    @Jacksonized
    @Builder
    class None implements ServiceProtocolType {

        @Override
        public String formatUrl(String base) {
            return base;
        }

        @Override
        public void open(String url) {}

        @Override
        public String getTranslationKey() {
            return "none";
        }
    }

    @JsonTypeName("http")
    @Value
    @Jacksonized
    @Builder
    class Http implements ServiceProtocolType {

        String path;

        @Override
        public String formatUrl(String base) {
            var url = "http://" + base;
            if (path != null && !path.isEmpty()) {
                url += (!path.startsWith("/") ? "/" : "") + path;
            }
            return url;
        }

        @Override
        public void open(String url) {
            Hyperlinks.open(url);
        }

        @Override
        public String getTranslationKey() {
            return "http";
        }
    }

    @JsonTypeName("https")
    @Value
    @Jacksonized
    @Builder
    class Https implements ServiceProtocolType {

        String path;

        @Override
        public String formatUrl(String base) {
            var url = "https://" + base;
            if (path != null && !path.isEmpty()) {
                url += (!path.startsWith("/") ? "/" : "") + path;
            }
            return url;
        }

        @Override
        public void open(String url) {
            Hyperlinks.open(url);
        }

        @Override
        public String getTranslationKey() {
            return "https";
        }
    }
}
