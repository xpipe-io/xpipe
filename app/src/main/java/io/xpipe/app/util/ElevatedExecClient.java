package io.xpipe.app.util;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BeaconClient;
import io.xpipe.app.beacon.BeaconClientInformation;
import io.xpipe.app.beacon.api.ElevatedExecCallbackExchange;
import io.xpipe.app.beacon.api.ElevatedExecQueryExchange;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.LocalShell;

public class ElevatedExecClient {

    public static void runLoop() {
        if (OsType.ofLocal() != OsType.WINDOWS) {
            return;
        }

        if (AppBeaconServer.get() != null) {
            return;
        }

        try {
            var port = AppProperties.get().queryEffectiveBeaconPort(true).orElse(AppProperties.get().getDefaultBeaconPort());
            var client = BeaconClient.establishConnection(port, BeaconClientInformation.Daemon.builder().build(),
                    AppProperties.get().getBeaconAuthFile());

            while (true) {
                ElevatedExecQueryExchange.Response response = client.performRequest(ElevatedExecQueryExchange.Request.builder().build());
                if (response.getId() == null) {
                    break;
                }

                var sc = LocalShell.getLocalShell(response.getDialect());
                var command = sc.command(response.getCommand());

                var out = command.readStdoutAndStderr();
                client.performRequest(ElevatedExecCallbackExchange.Request.builder()
                        .id(response.getId())
                        .stdout(out[0])
                        .stderr(out[1])
                        .exitCode(command.getExitCode())
                        .build());
            }
        } catch (Throwable e) {
            ErrorEventFactory.fromThrowable(e).expected().omit().term().handle();
        }
    }
}
