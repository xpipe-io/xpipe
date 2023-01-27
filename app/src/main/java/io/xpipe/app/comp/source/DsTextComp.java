package io.xpipe.app.comp.source;

import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextArea;

import java.util.List;

public class DsTextComp extends Comp<CompStructure<TextArea>> {

    private final ObservableValue<List<String>> value;

    public DsTextComp(ObservableValue<List<String>> value) {
        this.value = value;
    }

    private void setupListener(TextArea ta) {
        ChangeListener<List<String>> listener = (c, o, n) -> {
            ta.textProperty().setValue(String.join("\n", n));
        };
        value.addListener(listener);
        listener.changed(value, null, value.getValue());
    }

    @Override
    public CompStructure<TextArea> createBase() {
        var ta = new TextArea();
        setupListener(ta);
        return new SimpleCompStructure<>(ta);
    }
}
