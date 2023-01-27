package io.xpipe.cli.util;

import io.xpipe.beacon.ClientException;
import io.xpipe.core.dialog.BaseQueryElement;
import io.xpipe.core.dialog.DialogCancelException;

public class QueryHandler {

    private final BaseQueryElement element;

    public QueryHandler(BaseQueryElement element) {
        this.element = element;
    }

    public String getOverride() {
        if (element.getDescription() != null
                && ConfigOverride.get(element.getDescription()).isPresent()) {
            return ConfigOverride.get(element.getDescription()).get();
        }
        return null;
    }

    public String handle() throws ClientException, DialogCancelException {
        var fixed = QuietOverride.get() || !CliHelper.canHaveUserInput() || element.isQuiet();
        if (!fixed) {
            render();
        }

        var override = getOverride();
        if (override != null) {
            return override;
        }

        if (element.isRequired() && element.getValue() == null && fixed) {
            throw new ClientException("Missing required config parameter: " + element.getDescription());
        }

        if (fixed) {
            return element.getValue();
        }

        if (element.isSecret() && System.console() != null) {
            var read = new String(System.console().readPassword());
            if (read.length() > 0) {
                return read;
            }
        } else {
            var read = CliHelper.readLine();
            if (read != null) {
                return read;
            }
        }

        return element.getValue();
    }

    private void render() {
        var q = element;
        var prefix = q.isNewLine() ? "" : "> ";
        if (q.isRequired()) {
            if (q.getValue() != null && q.getValue().length() != 0) {
                System.out.print(q.getDescription() + ": ["
                        + (q.isSecret() ? "*".repeat(q.getValue().length()) : q.getValue()) + "] " + prefix);
            } else {
                System.out.print(q.getDescription() + ": " + prefix);
            }
        } else {
            var def = q.getValue() != null
                    ? (q.isSecret() ? "*".repeat(q.getValue().length()) : q.getValue())
                    : "none";
            System.out.print(q.getDescription() + ": [" + def + "] " + prefix);
        }

        if (q.isNewLine()) {
            System.out.println();
            System.out.print("> ");
        }
    }
}
