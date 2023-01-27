package io.xpipe.cli.util;

import io.xpipe.beacon.ClientException;
import io.xpipe.core.dialog.ChoiceElement;
import io.xpipe.core.dialog.DialogCancelException;

public class ChoiceHandler {

    private final ChoiceElement choiceElement;

    public ChoiceHandler(ChoiceElement choiceElement) {
        this.choiceElement = choiceElement;
    }

    public String getOverride() {
        if (choiceElement.getDescription() != null
                && ConfigOverride.get(choiceElement.getDescription()).isPresent()) {
            return ConfigOverride.get(choiceElement.getDescription()).get();
        }
        return null;
    }

    public String handle() throws ClientException, DialogCancelException {
        var available = choiceElement.getElements().stream()
                .filter(c -> !c.isDisabled())
                .toList();
        if (available.size() == 1) {
            return String.valueOf(choiceElement.getElements().indexOf(available.get(0)) + 1);
        }

        var fixed = QuietOverride.get() || !CliHelper.canHaveUserInput() || choiceElement.isQuiet();
        if (!fixed) {
            render();
        }

        var override = getOverride();
        if (override != null) {
            return override;
        }

        if (fixed) {
            return choiceElement.getSelected() != -1 ? "" + (choiceElement.getSelected() + 1) : null;
        }

        var read = CliHelper.readLine();
        if (read != null) {
            return read;
        }

        return choiceElement.getSelected() != -1 ? "" + (choiceElement.getSelected() + 1) : null;
    }

    private void render() {
        System.out.println(choiceElement.getDescription() + ":");
        for (int i = 0; i < choiceElement.getElements().size(); i++) {
            var selector = choiceElement.getElements().get(i).getCharacter() != null
                    ? choiceElement.getElements().get(i).getCharacter().toString()
                    : String.valueOf(i + 1);
            var prefix = "  " + selector + ") ";
            var suffix = choiceElement.getElements().get(i).isDisabled() ? " (Unavailable)" : "";
            var val = prefix + choiceElement.getElements().get(i).getDescription() + suffix;
            System.out.println(val);
        }

        var selected =
                choiceElement.getSelected() != -1 ? choiceElement.getElements().get(choiceElement.getSelected()) : null;
        var prefix = selected == null
                ? ":"
                : ": ["
                        + (selected.getCharacter() != null
                                ? selected.getCharacter().toString()
                                : String.valueOf(choiceElement.getSelected() + 1))
                        + "]";
        System.out.print(choiceElement.getDescription() + prefix + " > ");
    }
}
