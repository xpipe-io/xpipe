package io.xpipe.ext.base.desktop;

import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppExtensionManager;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.ChoiceComp;
import io.xpipe.app.fxcomps.impl.DataStoreChoiceComp;
import io.xpipe.app.fxcomps.impl.DataStoreListChoiceComp;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.store.DataStore;
import io.xpipe.ext.base.script.ScriptStore;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;

public class DesktopEnvironmentStoreProvider implements DataStoreProvider {

    @Override
    public ActionProvider.Action browserAction(BrowserSessionModel sessionModel, DataStoreEntry store, BooleanProperty busy) {
        return launchAction(store);
    }

    @Override
    public ActionProvider.Action activateAction(DataStoreEntry store) {
        return new ActionProvider.Action() {
            @Override
            public boolean requiresJavaFXPlatform() {
                return false;
            }

            @Override
            public void execute() throws Exception {
                DesktopEnvironmentStore s = store.getStore().asNeeded();
                var a = s.getBase().get().getProvider().activateAction(s.getBase().get());
                if (a != null) {
                    a.execute();
                }
            }
        };
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
                DesktopEnvironmentStore s = store.getStore().asNeeded();
                var a = s.getBase().get().getProvider().activateAction(s.getBase().get());
                if (a != null) {
                    a.execute();
                }
                var fullName = store.getName();
                s.runDesktopTerminal(fullName, null);
            }
        };
    }

    @Override
    public CreationCategory getCreationCategory() {
        return CreationCategory.DESKSTOP;
    }

    @Override
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        DesktopEnvironmentStore s = store.getStore().asNeeded();
        return s.getBase().get();
    }

    @Override
    @SneakyThrows
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        DesktopEnvironmentStore st = (DesktopEnvironmentStore) store.getValue();
        var host = new SimpleObjectProperty<>(st.getBase());
        var terminal = new SimpleObjectProperty<>(st.getTerminal());
        var dialect = new SimpleObjectProperty<>(st.getDialect());
        var scripts = new SimpleListProperty<>(FXCollections.observableArrayList(new ArrayList<>(st.getEffectiveScripts())));
        var initScript = new SimpleStringProperty(st.getInitScript());


        Comp<?> dialectChoice = (Comp<?>) Class.forName(
                        AppExtensionManager.getInstance()
                                .getExtendedLayer()
                                .findModule("io.xpipe.ext.proc")
                                .orElseThrow(),
                        "io.xpipe.ext.proc.ShellDialectChoiceComp")
                .getDeclaredConstructor(Property.class, boolean.class)
                .newInstance(dialect, false);
        return new OptionsBuilder()
                .nameAndDescription("desktopHost")
                .addComp(
                        new DataStoreChoiceComp<>(
                                DataStoreChoiceComp.Mode.HOST,
                                entry,
                                host,
                                DesktopBaseStore.class,
                                desktopStoreDataStoreEntryRef -> desktopStoreDataStoreEntryRef.getStore().supportsDesktopAccess(),
                                StoreViewState.get().getAllConnectionsCategory()),
                        host)
                .nonNull()
                .nameAndDescription("desktopTerminal")
                .addComp(ChoiceComp.ofTranslatable(terminal, ExternalTerminalType.getTypes(st.getUsedOsType(), true, false),true), terminal)
                .nonNull()
                .nameAndDescription("desktopShellDialect")
                .addComp(dialectChoice, dialect)
                .nonNull()
                .nameAndDescription("desktopSnippets")
                .addComp(
                        new DataStoreListChoiceComp<>(
                                scripts,
                                ScriptStore.class,
                                scriptStore -> !scripts.contains(scriptStore),
                                StoreViewState.get().getAllScriptsCategory()),
                        scripts)
                .nameAndDescription("desktopInitScript")
                .addComp(
                        new IntegratedTextAreaComp(
                                initScript,
                                false,
                                "commands",
                                Bindings.createStringBinding(
                                        () -> {
                                            return dialect.getValue() != null
                                                    ? dialect
                                                    .getValue()
                                                    .getScriptFileEnding()
                                                    : "sh";
                                        },
                                        dialect)),
                        initScript)
                .bind(
                        () -> {
                            return DesktopEnvironmentStore.builder().base(host.get()).terminal(terminal.get()).dialect(dialect.get()).scripts(scripts.get()).initScript(initScript.get()).build();
                        },
                        store)
                .buildDialog();
    }

    public String summaryString(StoreEntryWrapper wrapper) {
        DesktopEnvironmentStore s = wrapper.getEntry().getStore().asNeeded();
        return DataStoreFormatter.toApostropheName(s.getBase().get()) + " environment";
    }

    @Override
    public String getDisplayIconFileName(DataStore store) {
        return "base:desktopEnvironment_icon.svg";
    }

    @Override
    public DataStore defaultStore() {
        return DesktopEnvironmentStore.builder().build();
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("desktopEnvironment");
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(DesktopEnvironmentStore.class);
    }


}
