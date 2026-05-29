package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconInterface;

import io.xpipe.app.prefs.AppPrefs;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class TerminalPrepareExchange extends BeaconInterface<TerminalPrepareExchange.Request> {

    @Override
    public String getPath() {
        return "/terminal/prepare";
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) {
        var term = AppPrefs.get().terminalType().getValue();
        var unicode = term.supportsUnicode();
        var escapes = term.supportsEscapes();
        return Response.builder()
                .supportsUnicode(unicode)
                .supportsEscapeSequences(escapes)
                .build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID request;

        long pid;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        boolean supportsUnicode;
        boolean supportsEscapeSequences;
    }
}
