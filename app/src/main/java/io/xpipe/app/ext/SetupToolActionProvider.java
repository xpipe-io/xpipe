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

            if (!provider.get().checkInstalled()) {
                provider.get().execute();
            }

            var local = DataStorage.get().local();
            var sc = ((ShellStore) local.getStore()).getOrStartSession();
            var op = provider.get().getScan().create(local, sc);
            if (op != null && !op.isDisabled()) {
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
