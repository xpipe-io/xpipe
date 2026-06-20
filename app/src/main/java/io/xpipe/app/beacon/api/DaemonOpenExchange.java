package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconInterface;

import io.xpipe.app.beacon.BeaconServerException;
import io.xpipe.app.core.AppOpenArguments;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.platform.PlatformInit;
import io.xpipe.app.util.FilePath;
import io.xpipe.app.util.OsType;
import io.xpipe.app.util.ThreadHelper;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;
import java.util.List;

public class DaemonOpenExchange extends BeaconInterface<DaemonOpenExchange.Request> {

    private int openCounter = 0;

    @Override
    public String getPath() {
        return "/daemon/open";
    }

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconServerException {
        if (msg.getDirectory() != null && !AppProperties.get().getDataDir().equals(msg.getDirectory())) {
            ThreadHelper.runAsync(() -> {
                ThreadHelper.sleep(1000);
                AppOperationMode.close();
            });
            return Response.builder().restartRequired(true).build();
        }

        if (msg.getArguments().isEmpty()) {
            try {
                // At this point we are already loading this on another thread
                // so this call will only perform the waiting
                PlatformInit.init(true);
            } catch (Throwable t) {
                throw new BeaconServerException(t);
            }

            // The open command is used as a default opener on Linux
            // We don't want to overwrite the default startup mode
            if (OsType.ofLocal() == OsType.LINUX && openCounter++ == 0) {
                return Response.builder().build();
            }

            AppOperationMode.switchToAsync(AppOperationMode.GUI);
        } else {
            AppOpenArguments.handle(msg.getArguments());
        }
        return Response.builder().build();
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
        List<String> arguments;

        Path directory;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {

        boolean restartRequired;
    }
}
