package io.xpipe.app.ext;

import io.xpipe.app.storage.DataStorage;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
public enum DataStoreCreationCategory {
    HOST(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID, "ssh"),
    SHELL(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID, null),
    COMMAND(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID, null),
    TUNNEL(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID, "sshLocalTunnel"),
    SERVICE(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID, "customService"),
    SCRIPT(DataStorage.ALL_SCRIPTS_CATEGORY_UUID, "script"),
    SCRIPT_SOURCE(DataStorage.ALL_SCRIPTS_CATEGORY_UUID, "scriptCollectionSource"),
    CLUSTER(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID, null),
    DESKTOP(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID, null),
    SERIAL(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID, null),
    MACRO(DataStorage.ALL_MACROS_CATEGORY_UUID, null),
    FILE_SYSTEM(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID, null),
    IDENTITY(DataStorage.ALL_IDENTITIES_CATEGORY_UUID, "localIdentity"),
    NETWORK(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID, null);

    private final UUID category;
    private final String defaultProvider;
}
