package io.xpipe.app.beacon.impl;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.terminal.TerminalLauncherManager;
import io.xpipe.beacon.api.SshLaunchExchange;
import io.xpipe.app.process.ShellDialects;

import com.sun.net.httpserver.HttpExchange;

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
            return Response.builder().command(List.of()).build();
        }

        // There are sometimes multiple requests by a terminal client (e.g. Termius)
        // This might fail sometimes, but it is expected
        var r = TerminalLauncherManager.sshLaunchExchange();
        var c = ProcessControlProvider.get()
                .getEffectiveLocalDialect()
                .getOpenScriptCommand(r.toString())
                .buildBaseParts(null);
        return Response.builder().command(c).build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }
}
