package io.xpipe.app.identity;

import io.xpipe.app.storage.DataStoreAccessScope;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.store.ShellStore;

import javafx.beans.value.ObservableValue;

import lombok.Builder;
import lombok.Value;

import java.util.function.Supplier;

@Value
@Builder
public class SshIdentityStrategyChoiceConfig {

    Supplier<DataStoreAccessScope> scopeCheck;
    boolean allowKeyFileSync;
    ObservableValue<DataStoreEntryRef<ShellStore>> fileSystem;
}
