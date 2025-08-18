package io.xpipe.ext.base.service;

import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.util.Hyperlinks;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Locale;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ServiceProtocolType.Undefined.class),
    @JsonSubTypes.Type(value = ServiceProtocolType.Http.class),
    @JsonSubTypes.Type(value = ServiceProtocolType.Https.class),
    @JsonSubTypes.Type(value = ServiceProtocolType.Custom.class)
})
public interface ServiceProtocolType {

    String formatAddress(String base);

    void open(String url) throws Exception;

    String getTranslationKey();

    @JsonTypeName("none")
    @Value
    @Jacksonized
    @Builder
    class Undefined implements ServiceProtocolType {

        @Override
        public String formatAddress(String base) {
            return base;
        }

        @Override
        public void open(String url) {}

        @Override
        public String getTranslationKey() {
            return "undefined";
        }
    }

    @JsonTypeName("http")
    @Value
    @Jacksonized
    @Builder
    class Http implements ServiceProtocolType {

        String path;

        @Override
        public String formatAddress(String base) {
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
        public String formatAddress(String base) {
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

    @JsonTypeName("custom")
    @Value
    @Jacksonized
    @Builder
    class Custom implements ServiceProtocolType {

        String commandTemplate;

        @Override
        public String formatAddress(String base) {
            return base;
        }

        @Override
        public void open(String url) throws Exception {
            if (commandTemplate == null || commandTemplate.isBlank()) {
                return;
            }

            var port = url.split(":")[1];
            var format = commandTemplate.toLowerCase(Locale.ROOT).contains("$port")
                    ? commandTemplate
                    : commandTemplate + " localhost:$PORT";
            var toExecute = ExternalApplicationHelper.replaceVariableArgument(format, "PORT", port);
            // We can't be sure whether the command is blocking or not, so always make it not blocking
            ExternalApplicationHelper.startAsync(toExecute);
        }

        @Override
        public String getTranslationKey() {
            return "custom";
        }
    }
}
