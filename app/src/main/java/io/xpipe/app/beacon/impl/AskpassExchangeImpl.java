package io.xpipe.app.beacon.impl;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.terminal.TerminalView;
import io.xpipe.app.util.*;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.AskpassExchange;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.core.util.InPlaceSecretValue;
import javafx.beans.property.SimpleStringProperty;

import java.time.Duration;

public class AskpassExchangeImpl extends AskpassExchange {

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        // SSH auth with a smartcard will prompt to confirm user presence
        // Maybe we can show some dialog for this in the future
        if (msg.getPrompt() != null && msg.getPrompt().toLowerCase().contains("confirm user presence")) {
            var qe = new AppLayoutModel.QueueEntry(new SimpleStringProperty(msg.getPrompt()),
                    new LabelGraphic.IconGraphic("mdi2f-fingerprint"), () -> {});
            AppLayoutModel.get().getQueueEntries().add(qe);
            GlobalTimer.delay(() -> {
                AppLayoutModel.get().getQueueEntries().remove(qe);
            }, Duration.ofSeconds(3));
            return Response.builder().value(InPlaceSecretValue.of("")).build();
        }

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
        TerminalView.focus(term.get());
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }
}
