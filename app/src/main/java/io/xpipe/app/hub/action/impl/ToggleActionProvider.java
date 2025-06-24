package io.xpipe.app.hub.action.impl;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.ext.SingletonSessionStore;
import io.xpipe.app.hub.action.StoreAction;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class ToggleActionProvider implements ActionProvider {

    @Override
    public String getId() {
        return "toggleStore";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<SingletonSessionStore<?>> {

        private final boolean enabled;

        @Override
        public void executeImpl() throws Exception {
            if (enabled) {
                ref.getStore().startSessionIfNeeded();
            } else {
                ref.getStore().stopSessionIfNeeded();
            }
        }
    }
}
