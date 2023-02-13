package io.xpipe.ext.base;

import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;
import io.xpipe.core.impl.InMemoryStore;
import io.xpipe.core.store.DataStore;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.GuiDialog;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import io.xpipe.extension.util.SimpleValidator;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class InMemoryStoreProvider implements DataStoreProvider {

    @Override
    public boolean shouldShow() {
        return false;
    }

    @Override
    public GuiDialog guiDialog(Property<DataStore> store) {
        var val = new SimpleValidator();
        InMemoryStore st = (InMemoryStore) store.getValue();

        var charset = new SimpleObjectProperty<>(StreamCharset.UTF8);
        Property<String> valProp = new SimpleObjectProperty<>(
                st != null ? new String(st.getValue(), charset.getValue().getCharset()) : null);

        var q = new DynamicOptionsBuilder(I18n.observable("configuration"))
                .addCharset(charset)
                .addStringArea((String) null, valProp, false)
                .bind(
                        () -> {
                            return new InMemoryStore(
                                    valProp.getValue() != null
                                            ? valProp.getValue()
                                                    .getBytes(charset.getValue().getCharset())
                                            : null);
                        },
                        store)
                .buildComp();
        return new GuiDialog(q, val);
    }

    @Override
    public String queryInformationString(DataStore store, int length) throws Exception {
        return getDisplayName();
    }

    @Override
    public String toSummaryString(DataStore store, int length) {
        InMemoryStore s = store.asNeeded();
        return I18n.get("base.bytes", s.getValue().length);
    }

    @Override
    public Dialog dialogForStore(DataStore store) {
        InMemoryStore s = store.asNeeded();
        var userQ = Dialog.query(
                "Value", true, true, false, new String(s.getValue(), StandardCharsets.UTF_8), QueryConverter.STRING);
        return userQ.evaluateTo(() -> {
            byte[] bytes = ((String) userQ.getResult()).getBytes(StandardCharsets.UTF_8);
            return new InMemoryStore(bytes);
        });
    }

    @Override
    public DataCategory getCategory() {
        return DataCategory.STREAM;
    }

    @Override
    public DataStore defaultStore() {
        return new InMemoryStore(new byte[0]);
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("inMemory");
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(InMemoryStore.class);
    }
}
