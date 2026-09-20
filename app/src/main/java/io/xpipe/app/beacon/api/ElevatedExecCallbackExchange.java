package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;
import io.xpipe.app.util.ElevatedExec;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import java.util.UUID;

public class ElevatedExecCallbackExchange extends BeaconInterface<ElevatedExecCallbackExchange.Request> {

    @Override
    public String getPath() {
        return "/exec/elevated-callback";
    }

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Exception {
        var found = ElevatedExec.getElevatedExec(msg.getId());
        if (found.isEmpty()) {
            throw new BeaconClientException("No elevated exec found for " + msg.getId());
        } else {
            ElevatedExec.completeElevatedExec(msg.getId(), msg.getStdout(), msg.getStderr(), msg.getExitCode());
            return Response.builder().build();
        }
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
        UUID id;

        @NonNull
        String stdout;

        @NonNull
        String stderr;

        long exitCode;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
