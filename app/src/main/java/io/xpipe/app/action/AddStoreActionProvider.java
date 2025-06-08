package io.xpipe.app.action;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.store.StoreCreationDialog;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.DataStore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;
import java.util.UUID;

public class AddStoreActionProvider implements ActionProvider {

    @Override
    public String getId() {
        return "addStore";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends AbstractAction {

        DataStore store;

        @Override
        public void executeImpl() {
            if (store == null) {
                return;
            }

            var entry = DataStoreEntry.createNew(
                    UUID.randomUUID(),
                    StoreViewState.get()
                            .getActiveCategory()
                            .getValue()
                            .getCategory()
                            .getUuid(),
                    "",
                    store);
            StoreCreationDialog.showEdit(entry);
        }

        @Override
        public Map<String, String> toDisplayMap() {
            return Map.of();
        }
    }
}
