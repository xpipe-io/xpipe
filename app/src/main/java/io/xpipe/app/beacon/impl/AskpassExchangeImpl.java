package io.xpipe.app.beacon.impl;

import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.secret.SecretManager;
import io.xpipe.app.secret.SecretQueryState;
import io.xpipe.app.terminal.TerminalView;
import io.xpipe.app.util.*;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.AskpassExchange;
import io.xpipe.core.InPlaceSecretValue;

import javafx.beans.property.SimpleStringProperty;

import com.sun.net.httpserver.HttpExchange;

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
            var shown = AppLayoutModel.get().getQueueEntries().stream().anyMatch(queueEntry -> msg.getPrompt()
                    .equals(queueEntry.getName().getValue()));
            if (!shown) {
                var qe = new AppLayoutModel.QueueEntry(
                        new SimpleStringProperty(msg.getPrompt()),
                        new LabelGraphic.IconGraphic("mdi2f-fingerprint"),
                        () -> {});
                AppLayoutModel.get().getQueueEntries().add(qe);
                GlobalTimer.delay(
                        () -> {
                            AppLayoutModel.get().getQueueEntries().remove(qe);
                        },
                        Duration.ofSeconds(10));
            }
            return Response.builder().value(InPlaceSecretValue.of("")).build();
        }

        var prompt = msg.getPrompt();
        // sudo-rs uses a different prefix which we don't really need
        prompt = prompt.replace("[sudo: authenticate]", "[sudo]");

        if (msg.getRequest() == null) {
            var r = AskpassAlert.queryRaw(prompt, null, true);
            return Response.builder().value(r.getSecret()).build();
        }

        var found = msg.getSecretId() != null
                ? SecretManager.getProgress(msg.getRequest(), msg.getSecretId())
                : SecretManager.getProgress(msg.getRequest());
        if (found.isEmpty()) {
            throw new BeaconClientException("Unknown askpass request");
        }

        var p = found.get();
        var secret = p.process(prompt);
        if (p.getState() != SecretQueryState.NORMAL) {
            var ex = new BeaconClientException(SecretQueryState.toErrorMessage(p.getState()));
            ErrorEventFactory.preconfigure(ErrorEventFactory.fromThrowable(ex).ignore());
            throw ex;
        }
        focusTerminalIfNeeded(msg.getPid());
        return Response.builder().value(secret.inPlace()).build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
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
}
