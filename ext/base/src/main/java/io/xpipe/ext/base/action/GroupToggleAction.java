package io.xpipe.ext.base.action;

import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.GroupStore;
import lombok.Value;

public class GroupToggleAction implements ActionProvider {

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry entry;

        @Override
        public boolean requiresJavaFXPlatform() {
            return true;
        }

        @Override
        public void execute() throws Exception {
            entry.setExpanded(!entry.isExpanded());
        }
    }

    @Override
    public DefaultDataStoreCallSite<?> getDefaultDataStoreCallSite() {
        return new DefaultDataStoreCallSite<GroupStore>() {

            @Override
            public ActionProvider.Action createAction(GroupStore store) {
                return new Action(DataStorage.get().getStoreEntryIfPresent(store).orElseThrow());
            }

            @Override
            public Class<GroupStore> getApplicableClass() {
                return GroupStore.class;
            }
        };
    }
}
