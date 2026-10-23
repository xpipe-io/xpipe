package io.xpipe.app.storage;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@Jacksonized
@AllArgsConstructor
public class DataStoreCategoryConfig {

    @With
    DataStoreColor color;

    Boolean dontAllowScripts;
    Boolean confirmAllModifications;

    @With
    Boolean sync;

    Boolean confirmAllConfigurationChanges;
    UUID defaultIdentityStore;
    UUID defaultGatewayStore;

    public static DataStoreCategoryConfig empty() {
        return new DataStoreCategoryConfig(null, null, null, null, null, null, null);
    }

    public static DataStoreCategoryConfig merge(List<DataStoreCategoryConfig> configs) {
        DataStoreColor color = null;
        Boolean dontAllowScripts = null;
        Boolean warnOnAllModifications = null;
        Boolean sync = null;
        Boolean confirmAllConfigurationChanges = null;
        UUID defaultIdentityStore = null;
        UUID defaultGatewayStore = null;
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
            if (defaultGatewayStore == null) {
                defaultGatewayStore = config.defaultGatewayStore;
            }
            if (sync == null) {
                sync = config.sync;
            }
            if (confirmAllConfigurationChanges == null) {
                confirmAllConfigurationChanges = config.confirmAllConfigurationChanges;
            }
        }
        return new DataStoreCategoryConfig(
                color,
                dontAllowScripts,
                warnOnAllModifications,
                sync,
                confirmAllConfigurationChanges,
                defaultIdentityStore,
                defaultGatewayStore);
    }
}
