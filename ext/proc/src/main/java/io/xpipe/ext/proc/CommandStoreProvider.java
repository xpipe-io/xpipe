package io.xpipe.ext.proc;

import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.ShellType;
import io.xpipe.core.store.DataFlow;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.ext.proc.util.ShellHelper;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.GuiDialog;
import io.xpipe.extension.I18n;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.fxcomps.impl.DataStoreFlowChoiceComp;
import io.xpipe.extension.fxcomps.impl.ShellStoreChoiceComp;
import io.xpipe.extension.util.DataStoreFormatter;
import io.xpipe.extension.util.DialogHelper;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import io.xpipe.extension.util.SimpleValidator;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.List;

public class CommandStoreProvider implements DataStoreProvider {

    @Override
    public DataStore getParent(DataStore store) {
        CommandStore s = store.asNeeded();
        return s.getHost();
    }

    @Override
    public boolean isShareable() {
        return true;
    }

    @Override
    public String getId() {
        return "cmd";
    }

    @Override
    public GuiDialog guiDialog(Property<DataStore> store) {
        var val = new SimpleValidator();
        CommandStore st = (CommandStore) store.getValue();

        Property<ShellStore> machineProperty =
                new SimpleObjectProperty<>(st.getHost() != null ? st.getHost() : new LocalStore());
        ShellType type;
        try {
            type = machineProperty.getValue().determineType();
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
            type = null;
        }
        Property<ShellType> shellTypeProperty =
                new SimpleObjectProperty<>(st.getShell() != null ? st.getShell() : type);
        Property<String> commandProp = new SimpleObjectProperty<>(st.getCmd());
        Property<DataFlow> flowProperty = new SimpleObjectProperty<>(st.getFlow());
        var requiresElevationProperty = new SimpleBooleanProperty(st.isRequiresElevation());

        var q = new DynamicOptionsBuilder(I18n.observable("configuration"))
                .addComp(
                        I18n.observable("proc.host"),
                        ShellStoreChoiceComp.host(machineProperty),
                        machineProperty)
                .nonNull(val)
                .addComp(
                        I18n.observable("proc.shellType"),
                        new ShellTypeChoiceComp(shellTypeProperty),
                        shellTypeProperty)
                .addStringArea("proc.command", commandProp, false)
                .nonNull(val)
                .addComp("proc.usage", new DataStoreFlowChoiceComp(flowProperty, DataFlow.values()), flowProperty)
                .addToggle("requiresElevation", requiresElevationProperty)
                .bind(
                        () -> {
                            return CommandStore.builder()
                                    .cmd(commandProp.getValue())
                                    .host(machineProperty.getValue())
                                    .shell(shellTypeProperty.getValue())
                                    .flow(flowProperty.getValue())
                                    .requiresElevation(requiresElevationProperty.get())
                                    .build();
                        },
                        store)
                .buildComp();
        return new GuiDialog(q, val);
    }

    @Override
    public String toSummaryString(DataStore store, int length) {
        CommandStore s = store.asNeeded();
        return DataStoreFormatter.formatSubHost(
                l -> DataStoreFormatter.cut(s.getCmd().lines().findFirst().orElse("?"), l), s.getHost(), length);
    }

    @Override
    public String queryInformationString(DataStore store, int length) throws Exception {
        CommandStore s = store.asNeeded();
        var shellName = s.getShell() != null ? s.getShell().getDisplayName() : "Default Shell";
        return String.format("%s Command", shellName);
    }

    @Override
    public Category getCategory() {
        return Category.STREAM;
    }

    @Override
    public DataStore defaultStore() {
        return CommandStore.builder()
                .host(new LocalStore())
                .flow(DataFlow.INPUT)
                .build();
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("cmd", "command", "shell", "run", "execute");
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(CommandStore.class);
    }

    @Override
    public Dialog dialogForStore(DataStore store) {
        CommandStore commandStore = store.asNeeded();
        var cmdQ = Dialog.query("Command", true, true, false, commandStore.getCmd(), QueryConverter.STRING);
        var machineQ = DialogHelper.shellQuery("Command Host", commandStore.getHost());
        var shellQuery = Dialog.lazy(() -> {
            var available = ShellHelper.queryAvailableTypes(machineQ.getResult());
            return Dialog.choice(
                    "Shell Type",
                    t -> t.getDisplayName(),
                    true,
                    false,
                    available.get(0),
                    available.toArray(ShellType[]::new));
        });
        var flowQuery = DialogHelper.dataStoreFlowQuery(commandStore.getFlow(), DataFlow.values());

        return Dialog.chain(cmdQ, machineQ, shellQuery, flowQuery).evaluateTo(() -> {
            return CommandStore.builder()
                    .cmd(cmdQ.getResult())
                    .host(machineQ.getResult())
                    .shell(shellQuery.getResult())
                    .flow(flowQuery.getResult())
                    .build();
        });
    }
}
