package io.xpipe.app.ext;

import io.xpipe.app.storage.DataStorage;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
public enum DataStoreCreationCategory {
    HOST(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID),
    SHELL(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID),
    COMMAND(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID),
    TUNNEL(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID),
    SERVICE(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID),
    SCRIPT(DataStorage.ALL_SCRIPTS_CATEGORY_UUID),
    CLUSTER(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID),
    DESKTOP(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID),
    SERIAL(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID),
    MACRO(DataStorage.ALL_MACROS_CATEGORY_UUID),
    FILE_SYSTEM(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID),
    IDENTITY(DataStorage.ALL_IDENTITIES_CATEGORY_UUID);

    private final UUID category;
}
