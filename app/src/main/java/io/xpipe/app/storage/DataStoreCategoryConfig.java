package io.xpipe.app.storage;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class DataStoreCategoryConfig {

    public static DataStoreCategoryConfig empty() {
        return new DataStoreCategoryConfig(null, null, null, null, null, null);
    }

    public static DataStoreCategoryConfig merge(List<DataStoreCategoryConfig> configs) {
        DataStoreColor color = null;
        Boolean dontAllowScripts = null;
        Boolean warnOnAllModifications = null;
        Boolean sync = null;
        Boolean readOnly = null;
        UUID defaultIdentityStore = null;
        for (int i = configs.size() - 1; i >= 0; i--) {
            var config = configs.get(i);
            if (color == null) {
                color = config.color;
            }
            if (dontAllowScripts == null) {
                dontAllowScripts = config.dontAllowScripts;
            }
            if (warnOnAllModifications == null) {
                warnOnAllModifications = config.confirmAllModifications;
            }
            if (defaultIdentityStore == null) {
                defaultIdentityStore = config.defaultIdentityStore;
            }
            if (sync == null) {
                sync = config.sync;
            }
            if (readOnly == null) {
                readOnly = config.readOnly;
            }
        }
        return new DataStoreCategoryConfig(color, dontAllowScripts, warnOnAllModifications, sync, readOnly, defaultIdentityStore);
    }

    @With
    DataStoreColor color;

    Boolean dontAllowScripts;

    Boolean confirmAllModifications;

    @With
    Boolean sync;

    Boolean readOnly;

    UUID defaultIdentityStore;
}
