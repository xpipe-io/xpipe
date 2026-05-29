package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconInterface;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppVersion;
import io.xpipe.app.util.LicenseProvider;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class DaemonVersionExchange extends BeaconInterface<DaemonVersionExchange.Request> {

    @Override
    public String getPath() {
        return "/daemon/version";
    }

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) {
        var jvmVersion = System.getProperty("java.vm.vendor") + " " + System.getProperty("java.vm.name") + " ("
                + System.getProperty("java.vm.version") + ")";
        var version = AppProperties.get().getVersion();
        return Response.builder()
                .version(version)
                .canonicalVersion(AppVersion.parse(version)
                        .map(appVersion -> appVersion.toString())
                        .orElse("?"))
                .buildVersion(AppProperties.get().getBuild())
                .jvmVersion(jvmVersion)
                .plan(LicenseProvider.get().getLicenseId())
                .build();
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

        @NonNull
        String version;

        @NonNull
        String canonicalVersion;

        @NonNull
        String buildVersion;

        @NonNull
        String jvmVersion;

        @NonNull
        String plan;
    }
}
