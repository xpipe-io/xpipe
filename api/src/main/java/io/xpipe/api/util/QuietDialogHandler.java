package io.xpipe.api.util;

import io.xpipe.beacon.BeaconConnection;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.cli.DialogExchange;
import io.xpipe.core.dialog.BaseQueryElement;
import io.xpipe.core.dialog.ChoiceElement;
import io.xpipe.core.dialog.DialogElement;
import io.xpipe.core.dialog.DialogReference;

import java.util.Map;
import java.util.UUID;

public class QuietDialogHandler {

    private final UUID dialogKey;
    private DialogElement element;
    private final BeaconConnection connection;
    private final Map<String, String> overrides;

    public QuietDialogHandler(DialogReference ref, BeaconConnection connection, Map<String, String> overrides) {
        this.dialogKey = ref.getDialogId();
        this.element = ref.getStart();
        this.connection = connection;
        this.overrides = overrides;
    }

    public void handle() throws ClientException {
        String response = null;

        if (element instanceof ChoiceElement c) {
            response = handleChoice(c);
        }

        if (element instanceof BaseQueryElement q) {
            response = handleQuery(q);
        }

        DialogExchange.Response res = connection.performSimpleExchange(
                DialogExchange.Request.builder().dialogKey(dialogKey).value(response).build());
        if (res.getElement() != null && element.equals(res.getElement())) {
            throw new ClientException("Invalid value for key " + res.getElement().toDisplayString());
        }

        element = res.getElement();

        if (element != null) {
            handle();
        }
    }

    private String handleQuery(BaseQueryElement q) {
        if (q.isRequired() && !overrides.containsKey(q.getDescription())) {
            throw new IllegalStateException("Missing required config parameter: " + q.getDescription());
        }

        return overrides.get(q.getDescription());
    }

    private String handleChoice(ChoiceElement c) {
        if (c.isRequired() && !overrides.containsKey(c.getDescription())) {
            throw new IllegalStateException("Missing required config parameter: " + c.getDescription());
        }

        return overrides.get(c.getDescription());
    }
}
