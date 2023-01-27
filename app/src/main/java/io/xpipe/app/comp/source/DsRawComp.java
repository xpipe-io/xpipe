package io.xpipe.app.comp.source;

import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import io.xpipe.extension.fxcomps.util.SimpleChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextArea;

import java.util.HexFormat;

public class DsRawComp extends Comp<CompStructure<TextArea>> {

    private final ObservableValue<byte[]> value;

    public DsRawComp(ObservableValue<byte[]> value) {
        this.value = value;
    }

    private void setupListener(TextArea ta) {
        var format = HexFormat.of().withDelimiter(" ").withUpperCase();
        SimpleChangeListener.apply(PlatformThread.sync(value), val -> {
            ta.textProperty().setValue(format.formatHex(val));
        });
    }

    @Override
    public CompStructure<TextArea> createBase() {
        var ta = new TextArea();
        ta.setWrapText(true);
        setupListener(ta);
        return new SimpleCompStructure<>(ta);
    }
}
