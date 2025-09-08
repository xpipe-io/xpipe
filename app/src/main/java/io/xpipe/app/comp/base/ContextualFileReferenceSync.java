package io.xpipe.app.comp.base;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorAction;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.storage.DataStorageSyncHandler;
import io.xpipe.app.util.AsktextAlert;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContextualFileReferenceSync {

    public static ContextualFileReferenceSync of(Path dir, Function<Path, String> fileName, Supplier<Boolean> perUser) {
        return new ContextualFileReferenceSync(
                dir,
                path -> {
                    String name = fileName.apply(path);
                    while (true) {
                        var target = dir.resolve(name);
                        if (Files.exists(target)) {
                            var rename = new AtomicBoolean(false);
                            var event = ErrorEventFactory.fromMessage(AppI18n.get("syncFileExists", target))
                                    .customAction(new ErrorAction() {
                                        @Override
                                        public String getName() {
                                            return AppI18n.get("replaceFile");
                                        }

                                        @Override
                                        public String getDescription() {
                                            return AppI18n.get("replaceFileDescription");
                                        }

                                        @Override
                                        public boolean handle(ErrorEvent event) throws Exception {
                                            return true;
                                        }
                                    })
                                    .customAction(new ErrorAction() {
                                        @Override
                                        public String getName() {
                                            return AppI18n.get("renameFile");
                                        }

                                        @Override
                                        public String getDescription() {
                                            return AppI18n.get("renameFileDescription");
                                        }

                                        @Override
                                        public boolean handle(ErrorEvent event) throws Exception {
                                            rename.set(true);
                                            return true;
                                        }
                                    });
                            event.handle();

                            if (rename.get()) {
                                var newName = AsktextAlert.query(AppI18n.get("newFileName"), name);
                                if (newName.isEmpty()) {
                                    continue;
                                }

                                name = newName.get();
                                continue;
                            }
                        }

                        return target;
                    }
                },
                perUser);
    }

    Path targetDir;
    UnaryOperator<Path> targetLocation;
    Supplier<Boolean> perUser;

    public List<ContextualFileReferenceChoiceComp.PreviousFileReference> getExistingFiles() {
        var files = new ArrayList<ContextualFileReferenceChoiceComp.PreviousFileReference>();
        DataStorageSyncHandler.getInstance().getSavedDataFiles().forEach(path -> {
            if (!path.startsWith(targetDir)) {
                return;
            }

            files.add(new ContextualFileReferenceChoiceComp.PreviousFileReference(
                    path.getFileName().toString() + " (Git)", path));
        });
        return files;
    }
}
