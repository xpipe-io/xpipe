package io.xpipe.ext.base.identity.ssh;

import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import javafx.beans.property.Property;
import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;
import java.util.function.Predicate;

@Value
@Builder
public class SshIdentityStrategyChoiceConfig {

    Property<DataStoreEntryRef<ShellStore>> proxy;
    Predicate<Path> perUserKeyFileCheck;
    boolean allowKeyFileSync;
    boolean allowAgentForward;
}
