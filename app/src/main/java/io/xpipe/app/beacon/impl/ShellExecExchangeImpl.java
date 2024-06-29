package io.xpipe.app.beacon.impl;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.beacon.api.ShellExecExchange;

import com.sun.net.httpserver.HttpExchange;
import lombok.SneakyThrows;

import java.util.concurrent.atomic.AtomicReference;

public class ShellExecExchangeImpl extends ShellExecExchange {

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
}
