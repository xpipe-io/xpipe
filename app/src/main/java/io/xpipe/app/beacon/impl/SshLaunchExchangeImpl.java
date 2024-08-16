package io.xpipe.app.beacon.impl;

import io.xpipe.app.util.TerminalLauncherManager;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.SshLaunchExchange;
import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.core.process.ShellDialects;

import com.sun.net.httpserver.HttpExchange;

public class SshLaunchExchangeImpl extends SshLaunchExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Exception {
        var usedDialect = ShellDialects.ALL.stream()
                .filter(dialect -> dialect.getExecutableName().equalsIgnoreCase(msg.getArguments()))
                .findFirst();
        if (msg.getArguments() != null
                && usedDialect.isEmpty()
                && !msg.getArguments().contains("SSH_ORIGINAL_COMMAND")) {
            throw new BeaconClientException("Unexpected argument: " + msg.getArguments());
        }

        var r = TerminalLauncherManager.waitForNextLaunch();
        var c = ProcessControlProvider.get()
                .getEffectiveLocalDialect()
                .getOpenScriptCommand(r.toString())
                .buildBaseParts(null);
        return Response.builder().command(c).build();
    }
}
