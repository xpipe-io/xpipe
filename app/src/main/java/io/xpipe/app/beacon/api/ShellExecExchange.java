package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BeaconInterface;

import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class ShellExecExchange extends BeaconInterface<ShellExecExchange.Request> {

    @Override
    public String getPath() {
        return "/shell/exec";
    }

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var existing = AppBeaconServer.get().getCache().getShellSession(msg.getConnection());
        AtomicReference<String> out = new AtomicReference<>();
        AtomicReference<String> err = new AtomicReference<>();
        long exitCode;
        try (var command = existing.getControl().command(msg.getCommand()).start()) {
            var r = command.readStdoutAndStderr();
            out.set(r[0]);
            err.set(r[1]);
            command.close();
            exitCode = command.getExitCode();
        }
        return Response.builder()
                .stdout(out.get())
                .stderr(err.get())
                .exitCode(exitCode)
                .build();
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID connection;

        @NonNull
        String command;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        long exitCode;

        @NonNull
        String stdout;

        @NonNull
        String stderr;
    }
}
