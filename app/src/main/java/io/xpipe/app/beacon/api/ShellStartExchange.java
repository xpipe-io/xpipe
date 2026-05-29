package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;
import io.xpipe.app.beacon.BeaconShellSession;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.FilePath;
import io.xpipe.app.util.JacksonMapper;
import io.xpipe.app.util.OsType;

import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class ShellStartExchange extends BeaconInterface<ShellStartExchange.Request> {

    @Override
    public String getPath() {
        return "/shell/start";
    }

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var e = DataStorage.get()
                .getStoreEntryIfPresent(msg.getConnection())
                .orElseThrow(() -> new BeaconClientException("Unknown connection"));
        if (!(e.getStore() instanceof ShellStore s)) {
            throw new BeaconClientException("Not a shell connection");
        }

        var existing = AppBeaconServer.get().getCache().getShellSessions().stream()
                .filter(beaconShellSession -> beaconShellSession.getEntry().equals(e))
                .findFirst();
        var control = (existing.isPresent()
                ? existing.get().getControl()
                : s.standaloneControl().start());
        control.setNonInteractive();
        control.start();

        var d = control.getShellDialect().getDumbMode();
        if (!d.supportsAnyPossibleInteraction()) {
            control.close();
            d.throwIfUnsupported();
        }

        if (existing.isEmpty()) {
            AppBeaconServer.get().getCache().getShellSessions().add(new BeaconShellSession(e, control));
        }
        var ttyState =
                JacksonMapper.getDefault().valueToTree(control.getTtyState()).asText();
        return Response.builder()
                .shellDialect(control.getShellDialect().getId())
                .osType(control.getOsType())
                .osName(control.getOsName())
                .temp(control.getSystemTemporaryDirectory())
                .ttyState(ttyState)
                .build();
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID connection;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        String shellDialect;

        @NonNull
        OsType.Any osType;

        @NonNull
        String osName;

        @NonNull
        String ttyState;

        @NonNull
        FilePath temp;
    }
}
