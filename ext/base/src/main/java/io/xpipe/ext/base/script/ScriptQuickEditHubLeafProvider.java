package io.xpipe.ext.base.script;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.hub.comp.StoreCreationDialog;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.FileOpener;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

public class ScriptQuickEditHubLeafProvider implements HubLeafProvider<ScriptStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CUSTOM;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<ScriptStore> store) {
        return AppI18n.observable("edit");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<ScriptStore> store) {
        return new LabelGraphic.IconGraphic("mdal-edit");
    }

    @Override
    public Class<ScriptStore> getApplicableClass() {
        return ScriptStore.class;
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public AbstractAction createAction(DataStoreEntryRef<ScriptStore> store) {
        return Action.builder().ref(store).build();
    }

    @Override
    public String getId() {
        return "editScriptInEditor";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<ScriptStore> {

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

            var inPlace = ref.getStore().getTextSource() instanceof ScriptTextSource.InPlace;
            if (!inPlace) {
                StoreCreationDialog.showEdit(ref.get());
                return;
            }

            var script = ref.getStore();
            var dialect = script.getShellDialect();
            var ext = dialect != null ? dialect.getScriptFileEnding() : "sh";
            var name = OsFileSystem.ofLocal().makeFileSystemCompatible(ref.get().getName());
            FileOpener.openString(
                    name + "." + ext, this, script.getTextSource().getText().getValue(), (s) -> {
                        DataStorage.get()
                                .updateEntryStore(
                                        ref.get(),
                                        script.toBuilder()
                                                .textSource(ScriptTextSource.InPlace.builder()
                                                        .dialect(dialect)
                                                        .text(ShellScript.of(s))
                                                        .build())
                                                .build());
                    });
        }
    }
}
