package io.xpipe.app.comp.base;

import io.xpipe.app.browser.BrowserFileChooserSessionComp;
import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.FileSystemStore;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.ContextualFileReference;
import io.xpipe.app.storage.DataStorageSyncHandler;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.FilePath;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import lombok.NonNull;
import lombok.Setter;
import lombok.Value;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ContextualFileReferenceChoiceComp extends RegionBuilder<HBox> {

    private final Property<DataStoreEntryRef<? extends FileSystemStore>> fileSystem;
    private final Property<FilePath> filePath;
    private final ContextualFileReferenceSync sync;
    private final List<PreviousFileReference> previousFileReferences;
    private final Predicate<DataStoreEntry> filter;
    private final boolean directory;

    @Setter
    private ObservableValue<FilePath> prompt;

    public <T extends FileSystemStore> ContextualFileReferenceChoiceComp(
            ObservableValue<DataStoreEntryRef<T>> fileSystem,
            Property<FilePath> filePath,
            ContextualFileReferenceSync sync,
            List<PreviousFileReference> previousFileReferences,
            Predicate<DataStoreEntry> filter,
            boolean directory) {
        this.sync = sync;
        this.previousFileReferences = previousFileReferences;
        this.filter = filter;
        this.directory = directory;
        this.fileSystem = new SimpleObjectProperty<>();
        fileSystem.subscribe(val -> {
            this.fileSystem.setValue(val);
        });
        this.filePath = filePath;
    }

    @Override
    public HBox createSimple() {
        var path = previousFileReferences.isEmpty() ? createTextField() : createComboBox();
        var fileBrowseButton = new ButtonComp(null, new FontIcon("mdi2f-folder-open-outline"), () -> {
            var replacement = ProcessControlProvider.get().replace(fileSystem.getValue());
            BrowserFileChooserSessionComp.open(
                    () -> replacement,
                    () -> filePath.getValue() != null ? filePath.getValue().getParent() : null,
                    fileStore -> {
                        if (fileStore != null) {
                            filePath.setValue(fileStore.getPath());
                        }
                    },
                    false,
                    directory,
                    filter);
        });

        var gitShareButton = new ButtonComp(null, new FontIcon("mdi2g-git"), () -> {
            if (!DataStorageSyncHandler.getInstance().supportsSync()) {
                AppLayoutModel.get().selectSettings();
                AppPrefs.get().selectCategory("vaultSync");
                return;
            }

            var currentPath = filePath.getValue();
            if (currentPath == null) {
                return;
            }

            if (ContextualFileReference.of(currentPath).isInDataDirectory()) {
                return;
            }

            try {
                var rawSource = currentPath.asLocalPathIfPossible();
                if (rawSource.isEmpty() || !Files.exists(rawSource.get())) {
                    ErrorEventFactory.fromMessage("Unable to resolve local file path " + currentPath)
                            .expected()
                            .handle();
                    return;
                }

                var source = rawSource.get();
                var target = sync.getTargetLocation().apply(source);
                var shouldCopy = AppDialog.confirm("confirmGitShare");
                if (!shouldCopy) {
                    return;
                }

                var handler = DataStorageSyncHandler.getInstance();
                var syncedTarget =
                        handler.addDataFile(source, target, sync.getPerUser().get());

                var sourceBase = source.toString().endsWith(".pem")
                        ? Path.of(
                                source.toString().substring(0, source.toString().length() - 4))
                        : source;

                var pubSource = Path.of(sourceBase + ".pub");
                if (Files.exists(pubSource)) {
                    var pubTarget = Path.of(target.toString() + ".pub");
                    handler.addDataFile(pubSource, pubTarget, sync.getPerUser().get());
                }

                var ppkSource = Path.of(sourceBase + ".ppk");
                if (Files.exists(ppkSource)) {
                    var ppkTarget = Path.of(target.toString() + ".ppk");
                    handler.addDataFile(ppkSource, ppkTarget, sync.getPerUser().get());
                }

                Platform.runLater(() -> {
                    filePath.setValue(FilePath.of(syncedTarget));
                });
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).handle();
            }
        });
        gitShareButton.style("git-sync-file-button");
        gitShareButton.describe(d -> d.nameKey("gitShareFileTooltip"));
        gitShareButton.disable(Bindings.createBooleanBinding(
                () -> {
                    return filePath.getValue() == null
                            || ContextualFileReference.of(filePath.getValue()).isInDataDirectory();
                },
                filePath));

        var nodes = new ArrayList<BaseRegionBuilder<?, ?>>();
        nodes.add(path);
        nodes.add(fileBrowseButton);
        if (sync != null) {
            nodes.add(gitShareButton);
        }
        var layout = new InputGroupComp(nodes).setMainReference(path).apply(struc -> struc.setFillHeight(true));

        layout.apply(struc -> {
            struc.focusedProperty().addListener((observable, oldValue, newValue) -> {
                struc.getChildren().getFirst().requestFocus();
            });
        });

        return layout.build();
    }

    private BaseRegionBuilder<?, ?> createComboBox() {
        var allFiles = new ArrayList<>(previousFileReferences);
        allFiles.addAll(sync != null ? sync.getExistingFiles() : List.of());
        var items = allFiles.stream()
                .map(previousFileReference -> previousFileReference.getPath().toString())
                .toList();
        var prop = new SimpleStringProperty();
        filePath.subscribe(s -> PlatformThread.runLaterIfNeeded(() -> {
            prop.set(s != null ? s.toString() : null);
        }));
        prop.addListener((observable, oldValue, newValue) -> {
            filePath.setValue(newValue != null ? FilePath.of(newValue) : null);
        });
        var combo = new ComboTextFieldComp(prop, FXCollections.observableList(items), () -> {
            return new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        return;
                    }

                    var display = allFiles.stream()
                            .filter(ref -> ref.path.toString().equals(item))
                            .findFirst()
                            .map(previousFileReference -> previousFileReference.getDisplayName())
                            .orElse(item);
                    setText(display);
                }
            };
        });
        combo.setPrompt(Bindings.createStringBinding(
                () -> {
                    return filePath.getValue() != null ? filePath.getValue().toString() : null;
                },
                filePath));
        combo.hgrow();
        return combo;
    }

    private BaseRegionBuilder<?, ?> createTextField() {
        var prop = new SimpleStringProperty();
        filePath.subscribe(s -> PlatformThread.runLaterIfNeeded(() -> {
            prop.set(s != null ? s.toString() : null);
        }));
        prop.addListener((observable, oldValue, newValue) -> {
            filePath.setValue(newValue != null && !newValue.isBlank() ? FilePath.of(newValue.strip()) : null);
        });
        var fileNameComp = new TextFieldComp(prop).apply(struc -> HBox.setHgrow(struc, Priority.ALWAYS));

        if (prompt != null) {
            fileNameComp.apply(struc -> {
                prompt.subscribe(filePath -> {
                    PlatformThread.runLaterIfNeeded(() -> {
                        struc.setPromptText(filePath != null ? filePath.toString() : null);
                    });
                });
            });
        }

        return fileNameComp;
    }

    @Value
    public static class PreviousFileReference {

        String displayName;
        Path path;

        public static PreviousFileReference of(Path file) {
            return new PreviousFileReference(file.toString(), file);
        }
    }
}
