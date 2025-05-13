package io.xpipe.app.comp.base;

import io.xpipe.app.browser.BrowserFileChooserSessionComp;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.ContextualFileReference;
import io.xpipe.app.storage.DataStorageSyncHandler;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.store.FileSystemStore;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import atlantafx.base.theme.Styles;
import lombok.Value;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ContextualFileReferenceChoiceComp extends Comp<CompStructure<HBox>> {

    @Value
    public static class PreviousFileReference {

        String displayName;
        Path path;
    }

    private final Property<DataStoreEntryRef<? extends FileSystemStore>> fileSystem;
    private final Property<FilePath> filePath;
    private final ContextualFileReferenceSync sync;
    private final List<PreviousFileReference> previousFileReferences;

    public <T extends FileSystemStore> ContextualFileReferenceChoiceComp(
            Property<DataStoreEntryRef<T>> fileSystem,
            Property<FilePath> filePath,
            ContextualFileReferenceSync sync,
            List<PreviousFileReference> previousFileReferences) {
        this.sync = sync;
        this.previousFileReferences = previousFileReferences;
        this.fileSystem = new SimpleObjectProperty<>();
        fileSystem.subscribe(val -> {
            this.fileSystem.setValue(val);
        });
        this.fileSystem.addListener((observable, oldValue, newValue) -> {
            fileSystem.setValue(newValue != null ? newValue.asNeeded() : null);
        });
        this.filePath = filePath;
    }

    @Override
    public CompStructure<HBox> createBase() {
        var path = previousFileReferences.isEmpty() ? createTextField() : createComboBox();
        var fileBrowseButton = new ButtonComp(null, new FontIcon("mdi2f-folder-open-outline"), () -> {
                    var replacement = ProcessControlProvider.get().replace(fileSystem.getValue());
                    BrowserFileChooserSessionComp.openSingleFile(
                            () -> replacement,
                            () -> filePath.getValue() != null ? filePath.getValue().getParent() : null,
                            fileStore -> {
                                if (fileStore != null) {
                                    filePath.setValue(fileStore.getPath());
                                    fileSystem.setValue(fileStore.getFileSystem());
                                }
                            },
                            false);
                })
                .styleClass(sync != null ? Styles.CENTER_PILL : Styles.RIGHT_PILL)
                .grow(false, true);

        var gitShareButton = new ButtonComp(null, new FontIcon("mdi2g-git"), () -> {
            if (!AppPrefs.get().enableGitStorage().get()) {
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
                var source = currentPath.asLocalPath();
                if (!Files.exists(source)) {
                    ErrorEvent.fromMessage("Unable to resolve local file path " + source).expected().handle();
                    return;
                }

                var target = sync.getTargetLocation().apply(source);
                var shouldCopy = AppWindowHelper.showConfirmationAlert(
                        "confirmGitShareTitle", "confirmGitShareHeader", "confirmGitShareContent");
                if (!shouldCopy) {
                    return;
                }

                var handler = DataStorageSyncHandler.getInstance();
                var syncedTarget = handler.addDataFile(
                        source, target, sync.getPerUser().test(source));

                var pubSource = Path.of(source + ".pub");
                if (Files.exists(pubSource)) {
                    var pubTarget = sync.getTargetLocation().apply(pubSource);
                    DataStorageSyncHandler.getInstance()
                            .addDataFile(
                                    pubSource, pubTarget, sync.getPerUser().test(pubSource));
                }

                Platform.runLater(() -> {
                    filePath.setValue(FilePath.of(syncedTarget));
                });
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).handle();
            }
        });
        gitShareButton.tooltipKey("gitShareFileTooltip");
        gitShareButton.styleClass(Styles.RIGHT_PILL).grow(false, true);
        gitShareButton.disable(Bindings.createBooleanBinding(
                () -> {
                    return filePath.getValue() != null
                            && ContextualFileReference.of(filePath.getValue()).isInDataDirectory();
                },
                filePath));

        var nodes = new ArrayList<Comp<?>>();
        nodes.add(path);
        nodes.add(fileBrowseButton);
        if (sync != null) {
            nodes.add(gitShareButton);
        }
        var layout = new HorizontalComp(nodes).apply(struc -> struc.get().setFillHeight(true));

        layout.apply(struc -> {
            struc.get().focusedProperty().addListener((observable, oldValue, newValue) -> {
                struc.get().getChildren().getFirst().requestFocus();
            });
        });

        return new SimpleCompStructure<>(layout.createStructure().get());
    }

    private Comp<?> createComboBox() {
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
        var combo = new ComboTextFieldComp(prop, items, param -> {
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
        combo.hgrow();
        combo.styleClass(Styles.LEFT_PILL);
        combo.grow(false, true);
        return combo;
    }

    private Comp<?> createTextField() {
        var prop = new SimpleStringProperty();
        filePath.subscribe(s -> PlatformThread.runLaterIfNeeded(() -> {
            prop.set(s != null ? s.toString() : null);
        }));
        prop.addListener((observable, oldValue, newValue) -> {
            filePath.setValue(newValue != null && !newValue.isBlank() ? FilePath.of(newValue.strip()) : null);
        });
        var fileNameComp = new TextFieldComp(prop)
                .apply(struc -> HBox.setHgrow(struc.get(), Priority.ALWAYS))
                .styleClass(Styles.LEFT_PILL)
                .grow(false, true);
        return fileNameComp;
    }
}
