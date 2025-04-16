package io.xpipe.app.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class DataStoreCategoryConfig {

    public static DataStoreCategoryConfig empty() {
        return new DataStoreCategoryConfig(null, null, null, null, null);
    }

    public static DataStoreCategoryConfig merge(List<DataStoreCategoryConfig> configs) {
        DataStoreColor color = null;
        Boolean dontAllowScripts = null;
        Boolean warnOnAllModifications = null;
        Boolean sync = null;
        UUID defaultIdentityStore = null;
        for (DataStoreCategoryConfig config : configs) {
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
        }
        return new DataStoreCategoryConfig(color, dontAllowScripts, warnOnAllModifications, sync, defaultIdentityStore);
    }

    @With
    DataStoreColor color;

    Boolean dontAllowScripts;

    Boolean confirmAllModifications;

    @With
    Boolean sync;

    UUID defaultIdentityStore;
}
