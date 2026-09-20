package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconInterface;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.util.ElevatedExec;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class ElevatedExecQueryExchange extends BeaconInterface<ElevatedExecQueryExchange.Request> {

    @Override
    public String getPath() {
        return "/exec/elevated-query";
    }

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Exception {
        var found = ElevatedExec.getNextElevatedExec();
        if (found.isEmpty()) {
            return Response.builder().build();
        } else {
            return Response.builder().id(found.get().getId()).dialect(found.get().getDialect()).command(found.get().getCommand()).build();
        }
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {}

    @Jacksonized
    @Builder
    @Value
    public static class Response {

        UUID id;

        ShellDialect dialect;

        ShellScript command;
    }
}
