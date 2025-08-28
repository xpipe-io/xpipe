package io.xpipe.ext.base.script;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.hub.comp.StoreCreationDialog;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.FileOpener;
import io.xpipe.app.platform.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

public class SimpleScriptQuickEditHubLeafProvider implements HubLeafProvider<SimpleScriptStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CUSTOM;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<SimpleScriptStore> store) {
        return AppI18n.observable("base.edit");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<SimpleScriptStore> store) {
        return new LabelGraphic.IconGraphic("mdal-edit");
    }

    @Override
    public Class<SimpleScriptStore> getApplicableClass() {
        return SimpleScriptStore.class;
    }

    @Override
    public boolean isDefault(DataStoreEntryRef<SimpleScriptStore> o) {
        return true;
    }

    @Override
    public AbstractAction createAction(DataStoreEntryRef<SimpleScriptStore> store) {
        return Action.builder().ref(store).build();
    }

    @Override
    public String getId() {
        return "editScriptInEditor";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<SimpleScriptStore> {

        @Override
        public void executeImpl() {
            var predefined = DataStorage.get()
                            .getStoreCategoryIfPresent(ref.get().getCategoryUuid())
                            .map(category -> category.getUuid().equals(DataStorage.PREDEFINED_SCRIPTS_CATEGORY_UUID))
                            .orElse(false)
                    && Arrays.stream(PredefinedScriptStore.values())
                            .anyMatch(predefinedScriptStore -> predefinedScriptStore
                                    .getName()
                                    .equals(ref.get().getName()));
            if (predefined) {
                StoreCreationDialog.showEdit(ref.get());
                return;
            }

            var script = ref.getStore();
            var dialect = script.getMinimumDialect();
            var ext = dialect != null ? dialect.getScriptFileEnding() : "sh";
            var name = OsFileSystem.ofLocal().makeFileSystemCompatible(ref.get().getName());
            FileOpener.openString(name + "." + ext, this, script.getCommands(), (s) -> {
                ref.get().setStoreInternal(script.toBuilder().commands(s).build(), true);
            });
        }
    }
}
