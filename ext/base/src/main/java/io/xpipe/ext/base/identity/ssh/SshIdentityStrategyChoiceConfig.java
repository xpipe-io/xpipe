package io.xpipe.ext.base.identity.ssh;

import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;

import javafx.beans.value.ObservableValue;

import lombok.Builder;
import lombok.Value;

import java.util.function.Supplier;

@Value
@Builder
public class SshIdentityStrategyChoiceConfig {

    Supplier<Boolean> perUserKeyFileCheck;
    boolean allowKeyFileSync;
    boolean allowAgentForward;
    ObservableValue<DataStoreEntryRef<ShellStore>> fileSystem;
}
