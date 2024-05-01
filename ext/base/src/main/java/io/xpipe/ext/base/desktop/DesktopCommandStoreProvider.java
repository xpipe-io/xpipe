package io.xpipe.ext.base.desktop;

import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.fxcomps.impl.DataStoreChoiceComp;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.store.DataStore;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.List;

public class DesktopCommandStoreProvider implements DataStoreProvider {

    @Override
    public ActionProvider.Action browserAction(
            BrowserSessionModel sessionModel, DataStoreEntry store, BooleanProperty busy) {
        return launchAction(store);
    }

    @Override
    public ActionProvider.Action launchAction(DataStoreEntry store) {
        return new ActionProvider.Action() {
            @Override
            public boolean requiresJavaFXPlatform() {
                return false;
            }

            @Override
            public void execute() throws Exception {
                DesktopCommandStore s = store.getStore().asNeeded();
                var baseEntry = s.getEnvironment().getStore().getBase().get();
                var baseActivate = baseEntry.getProvider().activateAction(baseEntry);
                if (baseActivate != null) {
                    baseActivate.execute();
                }
                s.getEnvironment().getStore().runDesktopTerminal(store.getName(), s.getScript());
            }
        };
    }

    @Override
    public CreationCategory getCreationCategory() {
        return CreationCategory.DESKTOP;
    }

    @Override
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        DesktopCommandStore s = store.getStore().asNeeded();
        return s.getEnvironment().get();
    }

    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        DesktopCommandStore st = (DesktopCommandStore) store.getValue();
        var env = new SimpleObjectProperty<>(st.getEnvironment());
        var script = new SimpleStringProperty(st.getScript());
        return new OptionsBuilder()
                .nameAndDescription("desktopEnvironmentBase")
                .addComp(
                        new DataStoreChoiceComp<>(
                                DataStoreChoiceComp.Mode.HOST,
                                entry,
                                env,
                                DesktopEnvironmentStore.class,
                                desktopStoreDataStoreEntryRef ->
                                        desktopStoreDataStoreEntryRef.getStore().supportsDesktopAccess(),
                                StoreViewState.get().getAllConnectionsCategory()),
                        env)
                .nonNull()
                .nameAndDescription("desktopCommandScript")
                .addComp(
                        new IntegratedTextAreaComp(
                                script,
                                false,
                                "commands",
                                Bindings.createStringBinding(
                                        () -> {
                                            return env.getValue() != null
                                                            && env.getValue()
                                                                            .getStore()
                                                                            .getDialect()
                                                                    != null
                                                    ? env.getValue()
                                                            .getStore()
                                                            .getDialect()
                                                            .getScriptFileEnding()
                                                    : "sh";
                                        },
                                        env)),
                        script)
                .nonNull()
                .bind(
                        () -> {
                            return DesktopCommandStore.builder()
                                    .environment(env.get())
                                    .script(script.get())
                                    .build();
                        },
                        store)
                .buildDialog();
    }

    public String summaryString(StoreEntryWrapper wrapper) {
        DesktopCommandStore s = wrapper.getEntry().getStore().asNeeded();
        return DataStoreFormatter.toApostropheName(s.getEnvironment().get()) + " config";
    }

    @Override
    public String getDisplayIconFileName(DataStore store) {
        return "base:desktopCommand_icon.svg";
    }

    @Override
    public DataStore defaultStore() {
        return DesktopCommandStore.builder().build();
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("desktopCommand");
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(DesktopCommandStore.class);
    }
}
