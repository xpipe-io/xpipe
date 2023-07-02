package io.xpipe.ext.base;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.util.DialogHelper;
import io.xpipe.app.util.DynamicOptionsBuilder;
import io.xpipe.app.util.SimpleValidator;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.impl.SinkDrainStore;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import java.util.List;

public class SinkDrainStoreProvider implements DataStoreProvider {

    @Override
    public GuiDialog guiDialog(Property<DataStore> store) {
        var val = new SimpleValidator();
        SinkDrainStore st = (SinkDrainStore) store.getValue();

        var charset = new SimpleObjectProperty<>(st.getCharset());
        var newLine = new SimpleObjectProperty<>(st.getNewLine());

        var q = new DynamicOptionsBuilder(AppI18n.observable("configuration"))
                .addCharset(charset)
                .addNewLine(newLine)
                .bind(
                        () -> {
                            return SinkDrainStore.builder()
                                    .charset(st.getCharset())
                                    .newLine(st.getNewLine())
                                    .build();
                        },
                        store)
                .buildComp();
        return new GuiDialog(q, val);
    }

    @Override
    public boolean canManuallyCreate() {
        return false;
    }

    @Override
    public String queryInformationString(DataStore store, int length) {
        return null;
    }

    @Override
    public String toSummaryString(DataStore store, int length) {
        SinkDrainStore st = store.asNeeded();
        return switch (st.getState()) {
            case NONE_CONNECTED -> {
                yield AppI18n.get("unconnected");
            }
            case PRODUCER_CONNECTED -> {
                yield AppI18n.get("waitingForConsumer");
            }
            case CONSUMER_CONNECTED -> {
                yield AppI18n.get("waitingForProducer");
            }
            case OPEN -> {
                yield AppI18n.get("open");
            }
            case CLOSED -> {
                yield AppI18n.get("closed");
            }
        };
    }

    @Override
    public DataStore defaultStore() {
        return SinkDrainStore.builder()
                .charset(StreamCharset.UTF8)
                .newLine(NewLine.platform())
                .build();
    }

    @Override
    public Dialog dialogForStore(DataStore store) {
        SinkDrainStore st = store.asNeeded();
        var cs = DialogHelper.charsetQuery(st.getCharset(), false);
        var nl = DialogHelper.newLineQuery(st.getNewLine(), false);
        return Dialog.chain(cs, nl).evaluateTo(() -> SinkDrainStore.builder()
                .charset(cs.getResult())
                .newLine(nl.getResult())
                .build());
    }

    @Override
    public String getId() {
        return "sinkDrain";
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("sink_drain");
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(SinkDrainStore.class);
    }
}
