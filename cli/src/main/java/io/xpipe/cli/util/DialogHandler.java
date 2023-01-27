package io.xpipe.cli.util;

import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.cli.DialogExchange;
import io.xpipe.core.dialog.*;

import java.util.UUID;

public class DialogHandler {

    private final UUID dialogKey;
    private final XPipeCliConnection connection;
    private DialogElement element;

    public DialogHandler(DialogReference ref, XPipeCliConnection connection) {
        this.dialogKey = ref.getDialogId();
        this.element = ref.getStart();
        this.connection = connection;
    }

    public boolean handle() throws ClientException {
        var fixed = QuietOverride.get() || !CliHelper.canHaveUserInput();

        String response = null;
        boolean responseOverriden = false;

        if (element instanceof BusyElement) {
            BusySpinner.start(null, false);
        } else {
            BusySpinner.stop();
        }

        try {
            if (element instanceof ChoiceElement c) {
                var ch = new ChoiceHandler(c);
                response = ch.handle();
                responseOverriden = ch.getOverride() != null;
            }

            if (element instanceof BaseQueryElement q) {
                var qh = new QueryHandler(q);
                response = qh.handle();
                responseOverriden = qh.getOverride() != null;
            }
        } catch (DialogCancelException dce) {
            connection.performSimpleExchange(DialogExchange.Request.builder()
                    .dialogKey(dialogKey)
                    .cancel(true)
                    .build());
            return false;
        }

        if (element instanceof HeaderElement h && h.getHeader() != null && !QuietOverride.get()) {
            System.out.println(h.getHeader());
        }

        DialogExchange.Response res = connection.performSimpleExchange(DialogExchange.Request.builder()
                .dialogKey(dialogKey)
                .value(response)
                .build());
        if (fixed && responseOverriden && res.getElement() != null && element.equals(res.getElement())) {
            throw new ClientException(
                    "Invalid value for key " + res.getElement().toDisplayString());
        }

        element = res.getElement();

        if (element != null) {
            return handle();
        }

        return true;
    }
}
