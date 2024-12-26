package io.xpipe.app.beacon.impl;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.terminal.TerminalView;
import io.xpipe.app.util.AskpassAlert;
import io.xpipe.app.util.SecretManager;
import io.xpipe.app.util.SecretQueryState;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.AskpassExchange;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.core.process.OsType;

public class AskpassExchangeImpl extends AskpassExchange {

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        if (msg.getRequest() == null) {
            var r = AskpassAlert.queryRaw(msg.getPrompt(), null);
            return Response.builder().value(r.getSecret()).build();
        }

        var found = msg.getSecretId() != null
                ? SecretManager.getProgress(msg.getRequest(), msg.getSecretId())
                : SecretManager.getProgress(msg.getRequest());
        if (found.isEmpty()) {
            throw new BeaconClientException("Unknown askpass request");
        }

        var p = found.get();
        var secret = p.process(msg.getPrompt());
        if (p.getState() != SecretQueryState.NORMAL) {
            throw new BeaconClientException(SecretQueryState.toErrorMessage(p.getState()));
        }
        focusTerminalIfNeeded(msg.getPid());
        return Response.builder().value(secret.inPlace()).build();
    }

    private void focusTerminalIfNeeded(long pid) {
        if (TerminalView.get() == null) {
            return;
        }

        var found = TerminalView.get().findSession(pid);
        if (found.isEmpty()) {
            return;
        }

        var term = TerminalView.get().getTerminalInstances().stream()
                .filter(instance -> instance.equals(found.get().getTerminal()))
                .findFirst();
        if (term.isEmpty()) {
            return;
        }

        var control = term.get().controllable();
        if (control.isPresent()) {
            control.get().focus();
        } else {
            if (OsType.getLocal() == OsType.MACOS) {
                // Just focus the app, this is correct most of the time
                var terminalType = AppPrefs.get().terminalType().getValue();
                if (terminalType instanceof ExternalApplicationType.MacApplication m) {
                    m.focus();
                }
            }
        }
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }
}
