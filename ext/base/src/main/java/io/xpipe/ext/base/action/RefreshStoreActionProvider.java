package io.xpipe.ext.base.action;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.action.*;
import io.xpipe.core.store.DataStore;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class RefreshStoreActionProvider implements ActionProvider {


        @Override
    public String getId() {
        return "refreshStore";
    }
@Jacksonized
@SuperBuilder
    static class Action extends StoreAction<DataStore> {

        @Override
        public void executeImpl() {
            ref.get().validate();
        }
    }
}
