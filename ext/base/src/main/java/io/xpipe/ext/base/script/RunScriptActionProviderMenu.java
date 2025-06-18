package io.xpipe.ext.base.script;

import io.xpipe.app.action.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.hub.action.*;
import io.xpipe.app.hub.action.impl.RefreshHubLeafProvider;
import io.xpipe.app.hub.comp.StoreCategoryConfigComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.process.ShellTtyState;
import io.xpipe.core.process.SystemState;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

public class RunScriptActionProviderMenu implements HubBranchProvider<ShellStore>, BatchHubProvider<ShellStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CUSTOM;
    }

    @Value
    private static class TerminalRunActionProvider
            implements HubLeafProvider<ShellStore>, BatchHubProvider<ShellStore> {

        ScriptHierarchy hierarchy;

        @Override
        public RunTerminalScriptActionProvider.Action createBatchAction(DataStoreEntryRef<ShellStore> ref) {
            return RunTerminalScriptActionProvider.Action.builder()
                    .ref(ref)
                    .scriptStore(hierarchy.getLeafBase())
                    .build();
        }

        @Override
        public boolean isApplicable(DataStoreEntryRef<ShellStore> o) {
            return true;
        }

        @Override
        public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
            var t = AppPrefs.get().terminalType().getValue();
            return AppI18n.observable(
                    "executeInTerminal", t != null ? t.toTranslatedString().getValue() : "?");
        }

        @Override
        public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
            return new LabelGraphic.IconGraphic("mdi2c-code-greater-than");
        }

        @Override
        public Class<?> getApplicableClass() {
            return ShellStore.class;
        }

        @Override
        public ObservableValue<String> getName() {
            var t = AppPrefs.get().terminalType().getValue();
            return AppI18n.observable(
                    "executeInTerminal", t != null ? t.toTranslatedString().getValue() : "?");
        }

        @Override
        public LabelGraphic getIcon() {
            return new LabelGraphic.IconGraphic("mdi2c-code-greater-than");
        }
    }

    @Value
    private static class HubRunActionProvider implements HubLeafProvider<ShellStore>, BatchHubProvider<ShellStore> {

        ScriptHierarchy hierarchy;

        @Override
        public RunHubScriptActionProvider.Action createBatchAction(DataStoreEntryRef<ShellStore> ref) {
            return RunHubScriptActionProvider.Action.builder()
                    .ref(ref)
                    .scriptStore(hierarchy.getLeafBase())
                    .build();
        }

        @Override
        public AbstractAction createBatchAction(List<DataStoreEntryRef<ShellStore>> stores) {
            return RunHubBatchScriptActionProvider.Action.builder()
                    .refs(stores)
                    .scriptStore(hierarchy.getLeafBase())
                    .build();
        }

        @Override
        public boolean isApplicable(DataStoreEntryRef<ShellStore> o) {
            return true;
        }

        @Override
        public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
            return AppI18n.observable("runInConnectionHub");
        }

        @Override
        public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
            return new LabelGraphic.IconGraphic("mdi2d-desktop-mac");
        }

        @Override
        public Class<?> getApplicableClass() {
            return ShellStore.class;
        }

        @Override
        public ObservableValue<String> getName() {
            return AppI18n.observable("runInConnectionHub");
        }

        @Override
        public LabelGraphic getIcon() {
            return new LabelGraphic.IconGraphic("mdi2d-desktop-mac");
        }
    }

    @Value
    private static class BackgroundRunActionProvider
            implements HubLeafProvider<ShellStore>, BatchHubProvider<ShellStore> {

        ScriptHierarchy hierarchy;

        @Override
        public RunBackgroundScriptActionProvider.Action createBatchAction(DataStoreEntryRef<ShellStore> ref) {
            return RunBackgroundScriptActionProvider.Action.builder()
                    .ref(ref)
                    .scriptStore(hierarchy.getLeafBase())
                    .build();
        }

        @Override
        public boolean isApplicable(DataStoreEntryRef<ShellStore> o) {
            return true;
        }

        @Override
        public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
            return AppI18n.observable("executeInBackground");
        }

        @Override
        public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
            return new LabelGraphic.IconGraphic("mdi2f-flip-to-back");
        }

        @Override
        public Class<?> getApplicableClass() {
            return ShellStore.class;
        }

        @Override
        public ObservableValue<String> getName() {
            return AppI18n.observable("executeInBackground");
        }

        @Override
        public LabelGraphic getIcon() {
            return new LabelGraphic.IconGraphic("mdi2f-flip-to-back");
        }

        @Override
        public List<ActionProvider> getChildren(List<DataStoreEntryRef<ShellStore>> batch) {
            return List.of();
        }
    }

    @Value
    private static class ScriptActionProvider implements HubBranchProvider<ShellStore>, BatchHubProvider<ShellStore> {

        ScriptHierarchy hierarchy;

        @Override
        public ObservableValue<String> getName() {
            return new SimpleStringProperty(hierarchy.getBase().get().getName());
        }

        @Override
        public LabelGraphic getIcon() {
            if (hierarchy.isLeaf()) {
                return new LabelGraphic.ImageGraphic(hierarchy.getBase().get().getEffectiveIconFile(), 16);
            }

            return new LabelGraphic.IconGraphic("mdi2p-play-box-multiple-outline");
        }

        @Override
        public Class<ShellStore> getApplicableClass() {
            return ShellStore.class;
        }

        @Override
        public boolean isApplicable(DataStoreEntryRef<ShellStore> o) {
            return true;
        }

        @Override
        public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
            return getName();
        }

        @Override
        public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
            return getIcon();
        }

        @Override
        public List<HubMenuItemProvider<?>> getChildren(DataStoreEntryRef<ShellStore> store) {
            if (hierarchy.isLeaf()) {
                return List.of(
                        new TerminalRunActionProvider(hierarchy),
                        new HubRunActionProvider(hierarchy),
                        new BackgroundRunActionProvider(hierarchy));
            }

            return hierarchy.getChildren().stream()
                    .map(c -> new ScriptActionProvider(c))
                    .collect(Collectors.toList());
        }
    }

    private static class NoScriptsActionProvider implements HubLeafProvider<ShellStore>, BatchHubProvider<ShellStore> {

        @Override
        public void execute(List<DataStoreEntryRef<ShellStore>> dataStoreEntryRefs) throws Exception {
            var cat = StoreViewState.get().getAllScriptsCategory();
            cat.select();
        }

        @Override
        public void execute(DataStoreEntryRef<ShellStore> ref) {
            var cat = StoreViewState.get().getAllScriptsCategory();
            cat.select();
        }

        @Override
        public boolean isApplicable(DataStoreEntryRef<ShellStore> o) {
            return true;
        }

        @Override
        public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
            return AppI18n.observable("noScriptsAvailable");
        }

        @Override
        public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
            return new LabelGraphic.IconGraphic("mdi2i-image-filter-none");
        }

        @Override
        public ObservableValue<String> getName() {
            return AppI18n.observable("noScriptsAvailable");
        }

        @Override
        public LabelGraphic getIcon() {
            return new LabelGraphic.IconGraphic("mdi2i-image-filter-none");
        }

        @Override
        public Class<?> getApplicableClass() {
            return ShellStore.class;
        }
    }

    private static class ScriptsDisabledActionProvider
            implements HubLeafProvider<ShellStore>, BatchHubProvider<ShellStore> {

        @Override
        public void execute(List<DataStoreEntryRef<ShellStore>> dataStoreEntryRefs) throws Exception {
            var cat = StoreViewState.get()
                    .getCategoryWrapper(DataStorage.get()
                            .getStoreCategory(dataStoreEntryRefs.getFirst().get()));
            StoreCategoryConfigComp.show(cat);
        }

        @Override
        public void execute(DataStoreEntryRef<ShellStore> ref) {
            var cat = StoreViewState.get().getCategoryWrapper(DataStorage.get().getStoreCategory(ref.get()));
            StoreCategoryConfigComp.show(cat);
        }

        @Override
        public boolean isApplicable(DataStoreEntryRef<ShellStore> o) {
            return true;
        }

        @Override
        public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
            return AppI18n.observable("scriptsDisabled");
        }

        @Override
        public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
            return new LabelGraphic.IconGraphic("mdi2b-block-helper");
        }

        @Override
        public ObservableValue<String> getName() {
            return AppI18n.observable("scriptsDisabled");
        }

        @Override
        public LabelGraphic getIcon() {
            return new LabelGraphic.IconGraphic("mdi2b-block-helper");
        }

        @Override
        public Class<?> getApplicableClass() {
            return ShellStore.class;
        }
    }

    private static class NoStateActionProvider implements HubLeafProvider<ShellStore>, BatchHubProvider<ShellStore> {

        @Override
        public boolean isApplicable(DataStoreEntryRef<ShellStore> o) {
            return true;
        }

        @Override
        public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
            return AppI18n.observable("noScriptStateAvailable");
        }

        @Override
        public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
            return new LabelGraphic.IconGraphic("mdi2i-image-filter-none");
        }

        @Override
        public Class<?> getApplicableClass() {
            return ShellStore.class;
        }

        @Override
        public ObservableValue<String> getName() {
            return AppI18n.observable("noScriptStateAvailable");
        }

        @Override
        public LabelGraphic getIcon() {
            return new LabelGraphic.IconGraphic("mdi2i-image-filter-none");
        }

        @Override
        public StoreAction<ShellStore> createBatchAction(DataStoreEntryRef<ShellStore> ref) {
            return RefreshHubLeafProvider.Action.builder()
                    .ref(ref.asNeeded())
                    .build()
                    .asNeeded();
        }
    }

    @Override
    public Class<ShellStore> getApplicableClass() {
        return ShellStore.class;
    }

    @Override
    public boolean isMajor(DataStoreEntryRef<ShellStore> o) {
        return true;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<ShellStore> store) {
        return AppI18n.observable("runScript");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<ShellStore> store) {
        return new LabelGraphic.IconGraphic("mdi2p-play-box-multiple-outline");
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<ShellStore> o) {
        var state = o.get().getStorePersistentState();
        if (state instanceof SystemState systemState) {
            return (systemState.getShellDialect() == null
                            || systemState.getShellDialect().getDumbMode().supportsAnyPossibleInteraction())
                    && (systemState.getTtyState() == null || systemState.getTtyState() == ShellTtyState.NONE);
        } else {
            return false;
        }
    }

    @Override
    public List<HubMenuItemProvider<?>> getChildren(DataStoreEntryRef<ShellStore> store) {
        if (Boolean.TRUE.equals(
                DataStorage.get().getEffectiveCategoryConfig(store.get()).getDontAllowScripts())) {
            return List.of(new ScriptsDisabledActionProvider());
        }

        var replacement = ProcessControlProvider.get().replace(store);
        var state = replacement.get().getStorePersistentState();
        if (!(state instanceof SystemState systemState) || systemState.getShellDialect() == null) {
            return List.of(new NoStateActionProvider());
        }

        var hierarchy = ScriptHierarchy.buildEnabledHierarchy(ref -> {
            if (!ref.getStore().isRunnableScript()) {
                return false;
            }

            if (!ref.getStore().isCompatible(systemState.getShellDialect())) {
                return false;
            }

            return true;
        });
        List<HubMenuItemProvider<?>> list = hierarchy.getChildren().stream()
                .map(c -> new ScriptActionProvider(c))
                .collect(Collectors.toList());
        if (list.isEmpty()) {
            return List.of(new NoScriptsActionProvider());
        } else {
            return list;
        }
    }

    @Override
    public ObservableValue<String> getName() {
        return AppI18n.observable("runScript");
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2p-play-box-multiple-outline");
    }

    @Override
    public List<ActionProvider> getChildren(List<DataStoreEntryRef<ShellStore>> batch) {
        if (batch.stream()
                .anyMatch(store -> Boolean.TRUE.equals(DataStorage.get()
                        .getEffectiveCategoryConfig(store.get())
                        .getDontAllowScripts()))) {
            return List.of(new ScriptsDisabledActionProvider());
        }

        var stateMissing = batch.stream().anyMatch(ref -> {
            var state = ref.get().getStorePersistentState();
            if (state instanceof SystemState systemState) {
                if (systemState.getShellDialect() == null) {
                    return true;
                }

                if (systemState.getTtyState() == null || systemState.getTtyState() != ShellTtyState.NONE) {
                    return true;
                }
            }
            return false;
        });

        if (stateMissing) {
            return List.of(new NoStateActionProvider());
        }

        var hierarchy = ScriptHierarchy.buildEnabledHierarchy(scriptRef -> {
            var compatible = batch.stream().allMatch(ref -> {
                var state = ref.get().getStorePersistentState();
                if (state instanceof SystemState systemState) {
                    return scriptRef.getStore().isCompatible(systemState.getShellDialect());
                } else {
                    return false;
                }
            });
            if (!compatible) {
                return false;
            }

            if (!scriptRef.getStore().isRunnableScript()) {
                return false;
            }

            return true;
        });
        var list = hierarchy.getChildren().stream()
                .<ActionProvider>map(c -> new ScriptActionProvider(c))
                .toList();
        if (list.isEmpty()) {
            return List.of(new NoScriptsActionProvider());
        } else {
            return list;
        }
    }
}
