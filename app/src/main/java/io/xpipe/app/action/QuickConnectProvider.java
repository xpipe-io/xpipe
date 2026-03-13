package io.xpipe.app.action;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface QuickConnectProvider extends ActionProvider {

    static List<QuickConnectProvider> getAll() {
        return ActionProvider.ALL.stream().map(actionProvider -> actionProvider instanceof QuickConnectProvider qcp ? qcp : null).filter(Objects::nonNull).toList();
    }

    static Optional<QuickConnectProvider> find(String input) {
        return ActionProvider.ALL.stream().filter(actionProvider -> actionProvider instanceof QuickConnectProvider qcp &&
                (input.toLowerCase().startsWith(qcp.getName().toLowerCase()) || qcp.getName().toLowerCase().startsWith(input.toLowerCase())))
                .findFirst().map(qcp -> (QuickConnectProvider) qcp);
    }

    String getName();

    Optional<DataStoreEntry> findExisting(DataStore store);

    DataStore createStore(String arguments, DataStore existing);

    String getPlaceholder();

    boolean skipDialogIfPossible();

    default void open(DataStoreEntry e) throws Exception {
        e.getProvider().launch(e).run();
    }
}
