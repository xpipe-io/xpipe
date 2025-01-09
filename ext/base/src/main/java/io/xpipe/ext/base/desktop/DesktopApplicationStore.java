package io.xpipe.ext.base.desktop;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.store.DataStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@SuperBuilder
@Jacksonized
@JsonTypeName("desktopApplication")
public class DesktopApplicationStore implements DataStore {

    DataStoreEntryRef<DesktopBaseStore> desktop;
    String path;
    String arguments;

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(desktop);
        Validators.isType(desktop, DesktopBaseStore.class);
        desktop.checkComplete();
        Validators.nonNull(path);
    }

    public CommandBuilder getFullCommand() {
        var builder = CommandBuilder.of().addFile(path).add(arguments != null ? " " + arguments : "");
        builder = desktop.getStore().getUsedDesktopDialect().launchAsnyc(builder);
        return builder;
    }
}
