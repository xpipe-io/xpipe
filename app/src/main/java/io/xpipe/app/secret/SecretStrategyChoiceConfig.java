package io.xpipe.app.secret;

import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import javafx.beans.property.Property;
import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;
import java.util.function.Predicate;

@Value
@Builder
public class SecretStrategyChoiceConfig {

    boolean allowNone;
}
