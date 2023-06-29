package io.xpipe.ext.base;

import io.xpipe.app.comp.store.DsStreamStoreChoiceComp;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.app.util.DialogHelper;
import io.xpipe.app.util.SimpleValidator;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;

import java.util.List;

public class FileStoreProvider implements DataStoreProvider {

    @Override
    public boolean canManuallyCreate() {
        return false;
    }

    @Override
    public GuiDialog guiDialog(Property<DataStore> store) {
        var val = new SimpleValidator();
        var comp = new DsStreamStoreChoiceComp(store, null, false, false, DsStreamStoreChoiceComp.Mode.WRITE);
        return new GuiDialog(comp, val);
    }

    @Override
    public String queryInformationString(DataStore store, int length) {
        return getDisplayName();
    }

    @Override
    public String toSummaryString(DataStore store, int length) {
        FileStore s = store.asNeeded();
        var local = s.getFileSystem() instanceof LocalStore;
        if (local) {
            return fileNameString(s.getPath(), length);
        } else {
            var machineString = DataStoreFormatter.toName(s.getFileSystem(), length / 2);
            var fileString = fileNameString(s.getPath(), length - machineString.length() - 3);
            return String.format("%s @ %s", fileString, machineString);
        }
    }

    private String fileNameString(String input, int length) {
        if (input.length() <= length) {
            return input;
        }

        var rest = Math.max(0, length - 3);
        return input.substring(0, rest / 2) + "..." + input.substring(input.length() - (rest / 2));
    }

    @Override
    public String getDisplayName() {
        return DataStoreProvider.super.getDisplayName();
    }

    @Override
    public DataStore defaultStore() {
        return FileStore.builder().fileSystem(new LocalStore()).build();
    }

    @Override
    public Dialog dialogForStore(DataStore store) {
        FileStore fileStore = store.asNeeded();
        var machineQuery = DialogHelper.machineQuery(fileStore.getFileSystem());
        var fileQuery = DialogHelper.fileQuery(fileStore.getPath());
        return Dialog.chain(machineQuery, fileQuery).evaluateTo(() -> FileStore.builder()
                .fileSystem(machineQuery.getResult())
                .path(fileQuery.getResult())
                .build());
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("file");
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(FileStore.class);
    }
}
