package io.xpipe.app.hub.action.impl;

import io.xpipe.app.action.*;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.core.store.DataStore;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class RefreshActionProvider implements ActionProvider {

    @Override
    public String getId() {
        return "refreshStore";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<DataStore> {

        @Override
        public void executeImpl() {
            ref.get().validate();
        }
    }
}
