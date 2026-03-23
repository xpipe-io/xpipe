package io.xpipe.app.action;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.hub.action.impl.OpenHubMenuLeafProvider;
import io.xpipe.app.storage.DataStoreEntry;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface QuickConnectProvider extends ActionProvider {

    static List<QuickConnectProvider> getAll() {
        return ActionProvider.ALL.stream()
                .map(actionProvider -> actionProvider instanceof QuickConnectProvider qcp ? qcp : null)
                .filter(Objects::nonNull)
                .toList();
    }

    static Optional<QuickConnectProvider> find(String input) {
        return ActionProvider.ALL.stream()
                .filter(actionProvider -> actionProvider instanceof QuickConnectProvider qcp
                        && (input.length() <= qcp.getName().length()
                                        && input.toLowerCase()
                                                .startsWith(qcp.getName().toLowerCase())
                                || input.toLowerCase().startsWith(qcp.getName().toLowerCase())))
                .findFirst()
                .map(qcp -> (QuickConnectProvider) qcp);
    }

    String getName();

    Optional<DataStoreEntry> findExisting(DataStore store);

    DataStore createStore(String arguments, DataStore existing);

    String getPlaceholder();

    boolean skipDialogIfPossible();

    default void open(DataStoreEntry e) throws Exception {
        OpenHubMenuLeafProvider.Action.builder().ref(e.ref()).build().executeSync();
    }
}
