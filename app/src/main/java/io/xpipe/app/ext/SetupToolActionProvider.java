package io.xpipe.app.ext;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.storage.DataStorage;

import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashMap;
import java.util.Map;

public class SetupToolActionProvider implements ActionProvider {

    @Override
    public String getId() {
        return "setupTool";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends AbstractAction {

        private final String type;

        @Override
        @SneakyThrows
        public void executeImpl() {
            var provider = SetupProvider.byId(type);
            if (provider.isEmpty()) {
                throw ErrorEventFactory.expected(new IllegalArgumentException("Setup action not found: " + type));
            }

            var local = DataStorage.get().local();
            var sc = ((ShellStore) local.getStore()).getOrStartSession();
            var op = provider.get().getScan().create(local, sc);
            // Even if the op is disabled, we still add it as
            // any store we add should be able to set up itself
            if (op != null) {
                provider.get().getScan().scan(local, sc);
            }
        }

        @Override
        public Map<String, String> toDisplayMap() {
            var map = new LinkedHashMap<String, String>();
            map.put("Action", getDisplayName());
            map.put("Type", type);
            return map;
        }
    }
}
