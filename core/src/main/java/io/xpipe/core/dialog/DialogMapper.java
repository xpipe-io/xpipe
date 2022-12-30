package io.xpipe.core.dialog;

import java.util.LinkedHashMap;

public class DialogMapper {

    private final Dialog dialog;
    private final LinkedHashMap<String, String> map = new LinkedHashMap<>();

    public DialogMapper(Dialog dialog) {
        this.dialog = dialog;
        dialog.clearCompletion();
    }

    public LinkedHashMap<String, String> handle() throws Exception {
        var element = dialog.start();
        if (element == null) {
            return map;
        }

        handle(element);
        return map;
    }

    private void handle(DialogElement element) throws Exception {
        String response = null;
        if (element instanceof ChoiceElement c) {
            response = handleChoice(c);
        }

        if (element instanceof BaseQueryElement q) {
            response = handleQuery(q);
        }

        var newElement = dialog.next(response);
        if (element.equals(newElement)) {
            throw new IllegalStateException(
                    "Loop for key " + newElement.toDisplayString());
        }

        element = newElement;
        if (element != null) {
            handle(element);
        }
    }

    private String handleQuery(BaseQueryElement q) {
        map.put(q.getDescription(), q.getValue());
        return q.getValue();
    }

    private String handleChoice(ChoiceElement c) {
        map.put(c.getDescription(), c.getElements().get(c.getSelected()).getDescription());
        return String.valueOf(c.getSelected() + 1);
    }
}
