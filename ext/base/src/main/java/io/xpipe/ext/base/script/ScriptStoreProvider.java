package io.xpipe.ext.base.script;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.base.ListSelectorComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.*;
import io.xpipe.app.hub.comp.*;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.platform.Validator;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.*;
import io.xpipe.core.OsType;

import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;

import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public class ScriptStoreProvider implements DataStoreProvider {

    @Override
    public StoreEntryComp customEntryComp(StoreSection sec, boolean preferLarge) {
        if (sec.getWrapper().getValidity().getValue() == DataStoreEntry.Validity.LOAD_FAILED) {
            return StoreEntryComp.create(sec, null, preferLarge);
        }

        EnabledStoreState initialState = sec.getWrapper().getEntry().getStorePersistentState();
        var enabled = new SimpleBooleanProperty(initialState.isEnabled());
        sec.getWrapper().getPersistentState().subscribe((newValue) -> {
            EnabledStoreState s = sec.getWrapper().getEntry().getStorePersistentState();
            enabled.set(s.isEnabled());
        });

        var toggle = StoreToggleComp.<StatefulDataStore<EnabledStoreState>>enableToggle(
                null, sec, enabled, (s, aBoolean) -> {
                    var state = s.getState().toBuilder().enabled(aBoolean).build();
                    s.setState(state);
                });

        return StoreEntryComp.create(sec, toggle, preferLarge);
    }

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
    public BaseRegionBuilder<?, ?> stateDisplay(StoreEntryWrapper w) {
        return new SystemStateComp(new SimpleObjectProperty<>(SystemStateComp.State.SUCCESS));
    }

    @Override
    public DataStoreCreationCategory getCreationCategory() {
        return DataStoreCreationCategory.SCRIPT;
    }

    @SneakyThrows
    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        ScriptStore st = store.getValue().asNeeded();

        var textSource = new SimpleObjectProperty<>(
                st.getTextSource() != null
                        ? st.getTextSource()
                        : ScriptTextSource.InPlace.builder().build());
        var others =
                new SimpleListProperty<>(FXCollections.observableArrayList(new ArrayList<>(st.getEffectiveScripts())));

        var textSourceChoice = OptionsChoiceBuilder.builder()
                .property(textSource)
                .available(ScriptTextSource.getClasses())
                .allowNull(true)
                .build();

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
                FXCollections.observableList(vals), name, ignored -> null, selectedExecTypes, v -> false, () -> false);

        return new OptionsBuilder()
                .nameAndDescription("scriptSourceType")
                .sub(textSourceChoice.build(), textSource)
                .nameAndDescription("executionType")
                .documentationLink(DocumentationLink.SCRIPTING_TYPES)
                .addComp(selectorComp, selectedExecTypes)
                .check(validator ->
                        Validator.nonEmpty(validator, AppI18n.observable("executionType"), selectedExecTypes))
                .name("snippets")
                .description("snippetsDescription")
                .documentationLink(DocumentationLink.SCRIPTING_DEPENDENCIES)
                .addComp(
                        new StoreListChoiceComp<>(
                                others,
                                ScriptStore.class,
                                scriptStore -> !scriptStore.get().equals(entry) && !others.contains(scriptStore),
                                StoreViewState.get().getAllScriptsCategory()),
                        others)
                .bind(
                        () -> {
                            return ScriptStore.builder()
                                    .textSource(textSource.get())
                                    .scripts(new ArrayList<>(others.get()))
                                    .description(st.getDescription())
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
        ScriptStore st = wrapper.getEntry().getStore().asNeeded();
        var name = st.getShellDialect() != null ? st.getShellDialect().getExecutableName() : AppI18n.get("generic");
        return name + " " + AppI18n.get("script");
    }

    @Override
    public ObservableValue<String> informationString(StoreSection section) {
        ScriptStore st = section.getWrapper().getEntry().getStore().asNeeded();
        var init = st.isInitScript() ? AppI18n.get("init") : null;
        var file = st.isFileScript() ? AppI18n.get("fileBrowser") : null;
        var shell = st.isShellScript()
                ? AppI18n.get("shell")
                        + getShellSessionScriptName(section.getWrapper())
                                .map(s -> " " + s)
                                .orElse("")
                : null;
        var runnable = st.isRunnableScript() ? AppI18n.get("hub") : null;
        return new ReadOnlyObjectWrapper<>(
                new StoreStateFormat(List.of(), st.getTextSource().toSummary(), shell, init, file, runnable).format());
    }

    private Optional<String> getShellSessionScriptName(StoreEntryWrapper wrapper) {
        ScriptStore st = wrapper.getEntry().getStore().asNeeded();
        if (!st.isShellScript()) {
            return Optional.empty();
        }

        var name = wrapper.getName().getValue().toLowerCase(Locale.ROOT).replaceAll(" ", "_");
        if (st.getShellDialect() == null) {
            return Optional.of(OsFileSystem.of(OsType.LINUX).makeFileSystemCompatible(name) + ".sh");
        }

        var os = st.getShellDialect() == ShellDialects.CMD || ShellDialects.isPowershell(st.getShellDialect())
                ? OsType.WINDOWS
                : OsType.LINUX;
        return Optional.of(OsFileSystem.of(os).makeFileSystemCompatible(name) + "."
                + st.getShellDialect().getScriptFileEnding());
    }

    @SneakyThrows
    @Override
    public String getDisplayIconFileName(DataStore store) {
        if (store == null) {
            return "base:script_icon.svg";
        }

        ScriptStore st = store.asNeeded();
        return ShellDialectIcons.getImageName(st.getShellDialect());
    }

    @Override
    public DataStore defaultStore(DataStoreCategory category) {
        return ScriptStore.builder().scripts(List.of()).build();
    }

    @Override
    public String getId() {
        return "script";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(ScriptStore.class);
    }
}
