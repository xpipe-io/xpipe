package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.util.TerminalLauncherManager;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.SshLaunchExchange;
import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.core.process.ShellDialects;

import java.util.List;

public class SshLaunchExchangeImpl extends SshLaunchExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Exception {
        if ("echo $SHELL".equals(msg.getArguments())) {
            return Response.builder().command(List.of("echo", "/bin/bash")).build();
        }

        var usedDialect = ShellDialects.getStartableDialects().stream()
                .filter(dialect -> dialect.getExecutableName().equalsIgnoreCase(msg.getArguments()))
                .findFirst();
        if (msg.getArguments() != null
                && usedDialect.isEmpty()
                && !msg.getArguments().contains("SSH_ORIGINAL_COMMAND")) {
            throw new BeaconClientException("Unexpected argument: " + msg.getArguments());
        }

        // There are sometimes multiple requests by a terminal client (e.g. Termius)
        // This might fail sometimes, but it is expected
        var r = TerminalLauncherManager.waitForNextLaunch();
        var c = ProcessControlProvider.get()
                .getEffectiveLocalDialect()
                .getOpenScriptCommand(r.toString())
                .buildBaseParts(null);
        return Response.builder().command(c).build();
    }
}
