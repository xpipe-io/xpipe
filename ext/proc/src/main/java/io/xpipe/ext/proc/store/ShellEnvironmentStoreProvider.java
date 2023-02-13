package io.xpipe.ext.proc.store;

import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.ShellType;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.Identifiers;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.GuiDialog;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.impl.ShellStoreChoiceComp;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import io.xpipe.extension.util.SimpleValidator;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import java.util.List;

public class ShellEnvironmentStoreProvider implements DataStoreProvider {

    @Override
    public boolean isShareable() {
        return true;
    }

    @Override
    public DisplayCategory getDisplayCategory() {
        return DisplayCategory.COMMAND;
    }

    @Override
    public String getId() {
        return "shellEnvironment";
    }

    @Override
    public GuiDialog guiDialog(Property<DataStore> store) {
        var val = new SimpleValidator();
        ShellEnvironmentStore st = store.getValue().asNeeded();

        Property<ShellStore> hostProperty = new SimpleObjectProperty<>(st.getHost());
        Property<String> commandProp = new SimpleObjectProperty<>(st.getCommands());
        Property<ShellType> shellTypeProperty = new SimpleObjectProperty<>(st.getShell());

        var q = new DynamicOptionsBuilder(I18n.observable("configuration"))
                .addComp(I18n.observable("host"), ShellStoreChoiceComp.host(st, hostProperty), hostProperty)
                .nonNull(val)
                .addComp(
                        I18n.observable("proc.shellType"),
                        new ShellTypeChoiceComp(shellTypeProperty),
                        shellTypeProperty)
                .addComp(
                        I18n.observable("proc.commands"),
                        new IntegratedTextAreaComp(commandProp, false, "commands", "txt"),
                        commandProp)
                .nonNull(val)
                .bind(
                        () -> {
                            return new ShellEnvironmentStore(
                                    commandProp.getValue(), hostProperty.getValue(), shellTypeProperty.getValue());
                        },
                        store)
                .buildComp();
        return new GuiDialog(q, val);
    }

    @Override
    public String queryInformationString(DataStore store, int length) throws Exception {
        ShellEnvironmentStore s = store.asNeeded();
        var name = s.getShell() != null ? s.getShell().getDisplayName() : "Default";
        return I18n.get("shellEnvironment.informationFormat", name);
    }

    @Override
    public String toSummaryString(DataStore store, int length) {
        ShellEnvironmentStore s = store.asNeeded();
        var commandSummary = "<" + s.getCommands().lines().count() + " commands>";
        return commandSummary;
    }

    @Override
    public DataStore getParent(DataStore store) {
        ShellEnvironmentStore s = store.asNeeded();
        return s.getHost();
    }

    @Override
    public DataCategory getCategory() {
        return DataCategory.SHELL;
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(ShellEnvironmentStore.class);
    }

    @Override
    public DataStore defaultStore() {
        return new ShellEnvironmentStore(null, new LocalStore(), null);
    }

    @Override
    public List<String> getPossibleNames() {
        return Identifiers.get("shell", "environment");
    }
}
