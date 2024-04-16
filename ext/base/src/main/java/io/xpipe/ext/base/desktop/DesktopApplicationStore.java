package io.xpipe.ext.base.desktop;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.storage.ContextualFileReference;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonizedValue;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter
@SuperBuilder
@Jacksonized
@JsonTypeName("desktopApplication")
public class DesktopApplicationStore extends JacksonizedValue implements DataStore {

    private final DataStoreEntryRef<DesktopBaseStore> desktop;
    private final ContextualFileReference path;
    private final String arguments;

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(desktop);
        Validators.isType(desktop, DesktopBaseStore.class);
        Validators.nonNull(path);
    }

    public String getFullCommand() {
        var builder = CommandBuilder.of().addFile(path.toAbsoluteFilePath(null)).add(arguments != null ? " " + arguments : "");
        builder = desktop.getStore().getUsedDialect().launchAsnyc(builder);
        return builder.buildSimple();
    }
}
