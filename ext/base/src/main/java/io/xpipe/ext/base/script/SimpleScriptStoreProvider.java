package io.xpipe.ext.base.script;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.comp.base.ListSelectorComp;
import io.xpipe.app.core.AppExtensionManager;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppNames;
import io.xpipe.app.ext.*;
import io.xpipe.app.hub.comp.*;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.*;
import io.xpipe.core.OsType;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;

import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

public class SimpleScriptStoreProvider implements EnabledParentStoreProvider, DataStoreProvider {

    @Override
    public DocumentationLink getHelpLink() {
        return DocumentationLink.SCRIPTING;
    }

    @Override
    public boolean canMoveCategories() {
        return false;
    }

    @Override
    public boolean showProviderChoice() {
        return false;
    }

    @Override
    public boolean shouldShowScan() {
        return false;
    }

    @Override
    public Comp<?> stateDisplay(StoreEntryWrapper w) {
        return new SystemStateComp(new SimpleObjectProperty<>(SystemStateComp.State.SUCCESS));
    }

    @Override
    public DataStoreCreationCategory getCreationCategory() {
        return DataStoreCreationCategory.SCRIPT;
    }

    @Override
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        SimpleScriptStore st = store.getStore().asNeeded();
        return st.getGroup().get();
    }

    @SneakyThrows
    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        SimpleScriptStore st = store.getValue().asNeeded();

        var group = new SimpleObjectProperty<>(st.getGroup());
        Property<ShellDialect> dialect = new SimpleObjectProperty<>(st.getMinimumDialect());
        var others =
                new SimpleListProperty<>(FXCollections.observableArrayList(new ArrayList<>(st.getEffectiveScripts())));
        Property<String> commandProp = new SimpleObjectProperty<>(st.getCommands());

        var availableDialects = List.of(
                ShellDialects.SH,
                ShellDialects.BASH,
                ShellDialects.ZSH,
                ShellDialects.FISH,
                ShellDialects.CMD,
                ShellDialects.POWERSHELL,
                ShellDialects.POWERSHELL_CORE);
        Comp<?> choice = new ShellDialectChoiceComp(availableDialects, dialect, ShellDialectChoiceComp.NullHandling.NULL_IS_ALL);

        var vals = List.of(0, 1, 2, 3);
        var selectedStart = new ArrayList<Integer>();
        if (st.isInitScript()) {
            selectedStart.add(0);
        }
        if (st.isRunnableScript()) {
            selectedStart.add(1);
        }
        if (st.isFileScript()) {
            selectedStart.add(2);
        }
        if (st.isShellScript()) {
            selectedStart.add(3);
        }
        var name = new Function<Integer, String>() {

            @Override
            public String apply(Integer integer) {
                if (integer == 0) {
                    return AppI18n.get("initScript");
                }

                if (integer == 1) {
                    return AppI18n.get("runnableScript");
                }

                if (integer == 2) {
                    return AppI18n.get("fileScript");
                }

                if (integer == 3) {
                    return AppI18n.get("shellScript");
                }

                return "?";
            }
        };
        var selectedExecTypes = new SimpleListProperty<>(FXCollections.observableList(selectedStart));
        var selectorComp = new ListSelectorComp<>(
                FXCollections.observableList(vals), name, selectedExecTypes, v -> false, () -> false);

        return new OptionsBuilder()
                .name("minimumShellDialect")
                .description("minimumShellDialectDescription")
                .longDescription(DocumentationLink.SCRIPTING_COMPATIBILITY)
                .addComp(choice, dialect)
                .name("scriptContents")
                .description("scriptContentsDescription")
                .longDescription(DocumentationLink.SCRIPTING_EDITING)
                .addComp(
                        new IntegratedTextAreaComp(commandProp, false, "commands", Bindings.createStringBinding(() -> {
                            return dialect.getValue() != null
                                    ? dialect.getValue().getScriptFileEnding()
                                    : "sh";
                        })),
                        commandProp)
                .nameAndDescription("executionType")
                .longDescription(DocumentationLink.SCRIPTING_TYPES)
                .addComp(selectorComp, selectedExecTypes)
                .check(validator ->
                        Validator.nonEmpty(validator, AppI18n.observable("executionType"), selectedExecTypes))
                .name("snippets")
                .description("snippetsDescription")
                .longDescription(DocumentationLink.SCRIPTING_DEPENDENCIES)
                .addComp(
                        new StoreListChoiceComp<>(
                                others,
                                ScriptStore.class,
                                scriptStore -> !scriptStore.get().equals(entry) && !others.contains(scriptStore),
                                StoreViewState.get().getAllScriptsCategory()),
                        others)
                .name("scriptGroup")
                .description("scriptGroupDescription")
                .longDescription(DocumentationLink.SCRIPTING_GROUPS)
                .addComp(
                        new StoreChoiceComp<>(
                                StoreChoiceComp.Mode.OTHER,
                                null,
                                group,
                                ScriptGroupStore.class,
                                null,
                                StoreViewState.get().getAllScriptsCategory()),
                        group)
                .nonNull()
                .bind(
                        () -> {
                            return SimpleScriptStore.builder()
                                    .group(group.get())
                                    .minimumDialect(dialect.getValue())
                                    .scripts(new ArrayList<>(others.get()))
                                    .description(st.getDescription())
                                    .commands(commandProp.getValue())
                                    .initScript(selectedExecTypes.contains(0))
                                    .runnableScript(selectedExecTypes.contains(1))
                                    .fileScript(selectedExecTypes.contains(2))
                                    .shellScript(selectedExecTypes.contains(3))
                                    .build();
                        },
                        store)
                .buildDialog();
    }

    @Override
    public String summaryString(StoreEntryWrapper wrapper) {
        SimpleScriptStore st = wrapper.getEntry().getStore().asNeeded();
        if (!st.isShellScript()) {
            return null;
        }

        var name = wrapper.getName().getValue().toLowerCase(Locale.ROOT).replaceAll(" ", "_");
        if (st.getMinimumDialect() == null) {
            return OsFileSystem.of(OsType.LINUX).makeFileSystemCompatible(name) + ".sh";
        }

        var os = st.getMinimumDialect() == ShellDialects.CMD || ShellDialects.isPowershell(st.getMinimumDialect())
                ? OsType.WINDOWS
                : OsType.LINUX;
        return OsFileSystem.of(os).makeFileSystemCompatible(name) + "."
                + st.getMinimumDialect().getScriptFileEnding();
    }

    @Override
    public ObservableValue<String> informationString(StoreSection section) {
        SimpleScriptStore st = section.getWrapper().getEntry().getStore().asNeeded();
        var init = st.isInitScript() ? AppI18n.get("init") : null;
        var file = st.isFileScript() ? AppI18n.get("fileBrowser") : null;
        var shell = st.isShellScript() ? AppI18n.get("shell") : null;
        var runnable = st.isRunnableScript() ? AppI18n.get("hub") : null;
        var type = st.getMinimumDialect() != null
                ? st.getMinimumDialect().getDisplayName() + " " + AppI18n.get("script")
                : AppI18n.get("genericScript");
        var suffix = String.join(
                " / ",
                Stream.of(init, shell, file, runnable).filter(s -> s != null).toList());
        if (!suffix.isEmpty()) {
            suffix = "(" + suffix + ")";
        } else {
            suffix = null;
        }
        return new SimpleStringProperty(DataStoreFormatter.join(type, suffix));
    }

    @SneakyThrows
    @Override
    public String getDisplayIconFileName(DataStore store) {
        if (store == null) {
            return "proc:shellEnvironment_icon.svg";
        }

        SimpleScriptStore st = store.asNeeded();
        return ShellDialectChoiceComp.getImageName(st.getMinimumDialect());
    }

    @Override
    public DataStore defaultStore(DataStoreCategory category) {
        return SimpleScriptStore.builder().scripts(List.of()).build();
    }

    @Override
    public String getId() {
        return "script";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(SimpleScriptStore.class);
    }
}
