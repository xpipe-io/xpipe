package io.xpipe.ext.proc;

import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.DataStoreProviders;
import io.xpipe.extension.GuiDialog;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.impl.ShellStoreChoiceComp;
import io.xpipe.extension.util.DataStoreFormatter;
import io.xpipe.extension.util.DialogHelper;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import io.xpipe.extension.util.SimpleValidator;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import java.util.List;

public class ShellCommandStoreProvider implements DataStoreProvider {

    @Override
    public boolean isShareable() {
        return true;
    }

    @Override
    public String getId() {
        return "shellCommand";
    }

    @Override
    public GuiDialog guiDialog(Property<DataStore> store) {
        var val = new SimpleValidator();
        ShellCommandStore st = (ShellCommandStore) store.getValue();

        Property<ShellStore> hostProperty = new SimpleObjectProperty<>(st.getHost());
        Property<String> commandProp = new SimpleObjectProperty<>(st.getCmd());

        var q = new DynamicOptionsBuilder(I18n.observable("configuration"))
                .addComp(
                        I18n.observable("host"),
                        ShellStoreChoiceComp.host(st, hostProperty),
                        hostProperty)
                .nonNull(val)
                .addString(I18n.observable("proc.command"), commandProp)
                .nonNull(val)
                .bind(
                        () -> {
                            return new ShellCommandStore(commandProp.getValue(), hostProperty.getValue());
                        },
                        store)
                .buildComp();
        return new GuiDialog(q, val);
    }

    @Override
    public String queryInformationString(DataStore store, int length) throws Exception {
        ShellCommandStore s = store.asNeeded();
        return s.queryMachineName();
    }

    @Override
    public String toSummaryString(DataStore store, int length) {
        ShellCommandStore s = store.asNeeded();
        var local = ShellStore.isLocal(s.getHost());
        if (local) {
            return DataStoreFormatter.cut(s.getCmd(), length);
        } else {
            var machineString = DataStoreProviders.byStore(s.getHost()).toSummaryString(s.getHost(), length / 2);
            var fileString = DataStoreFormatter.cut(s.getCmd().toString(), length - machineString.length() - 3);
            return String.format("%s @ %s", fileString, machineString);
        }
    }

    @Override
    public Category getCategory() {
        return Category.SHELL;
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(ShellCommandStore.class);
    }

    @Override
    public DataStore defaultStore() {
        return new ShellCommandStore(null, new LocalStore());
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("shell_command");
    }

    @Override
    public Dialog dialogForStore(DataStore store) {
        ShellCommandStore s = store.asNeeded();
        var hostQ = DialogHelper.shellQuery("Command Host", s.getHost());
        var commandQ = Dialog.query("Command", true, true, false, s.getCmd(), QueryConverter.STRING);
        return Dialog.chain(hostQ, commandQ).evaluateTo(() -> {
            return new ShellCommandStore(commandQ.getResult(), hostQ.getResult());
        });
    }
}
