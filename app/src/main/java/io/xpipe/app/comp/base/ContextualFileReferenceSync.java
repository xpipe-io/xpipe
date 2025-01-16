package io.xpipe.app.comp.base;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageSyncHandler;

import lombok.Value;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

@Value
public class ContextualFileReferenceSync {

    Path existingFilesDir;
    Predicate<Path> perUser;
    UnaryOperator<Path> targetLocation;

    public List<ContextualFileReferenceChoiceComp.PreviousFileReference> getExistingFiles() {
        var dataDir = DataStorage.get().getDataDir();
        var files = new ArrayList<ContextualFileReferenceChoiceComp.PreviousFileReference>();
        DataStorageSyncHandler.getInstance().getSavedDataFiles().forEach(path -> {
            if (!path.startsWith(dataDir.resolve(existingFilesDir))) {
                return;
            }

            files.add(new ContextualFileReferenceChoiceComp.PreviousFileReference(
                    path.getFileName().toString() + " (Git)", path));
        });
        return files;
    }
}
