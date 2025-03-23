package io.xpipe.ext.base.script;

import io.xpipe.app.comp.store.StoreCreationComp;
import io.xpipe.app.comp.store.StoreCreationDialog;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.FileOpener;
import io.xpipe.core.process.OsType;

import lombok.Value;

import java.util.Arrays;

public class SimpleScriptQuickEditAction implements ActionProvider {
    @Override
    public DefaultDataStoreCallSite<?> getDefaultDataStoreCallSite() {
        return new DefaultDataStoreCallSite<SimpleScriptStore>() {
            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<SimpleScriptStore> store) {
                return new Action(store);
            }

            @Override
            public Class<SimpleScriptStore> getApplicableClass() {
                return SimpleScriptStore.class;
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntryRef<SimpleScriptStore> ref;

        @Override
        public void execute() {
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
            var ext = dialect.getScriptFileEnding();
            var name = OsType.getLocal().makeFileSystemCompatible(ref.get().getName());
            FileOpener.openString(name + "." + ext, this, script.getCommands(), (s) -> {
                ref.get().setStoreInternal(script.toBuilder().commands(s).build(), true);
            });
        }
    }
}
