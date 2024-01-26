package io.xpipe.app.fxcomps.impl;

import atlantafx.base.theme.Styles;
import io.xpipe.app.browser.StandaloneFileBrowser;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.ContextualFileReference;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FileSystemStore;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class ContextualFileReferenceChoiceComp extends SimpleComp {

    private final Property<DataStoreEntryRef<? extends FileSystemStore>> fileSystem;
    private final Property<String> filePath;

    public <T extends FileSystemStore> ContextualFileReferenceChoiceComp(
            ObservableValue<DataStoreEntryRef<T>> fileSystem, Property<String> filePath
    ) {
        this.fileSystem = new SimpleObjectProperty<>();
        SimpleChangeListener.apply(fileSystem, val -> {
            this.fileSystem.setValue(val);
        });
        this.filePath = filePath;
    }

    @Override
    protected Region createSimple() {
        var fileNameComp = new TextFieldComp(filePath).apply(struc -> HBox.setHgrow(struc.get(), Priority.ALWAYS)).styleClass(Styles.LEFT_PILL).grow(
                false, true);

        var fileBrowseButton = new ButtonComp(null, new FontIcon("mdi2f-folder-open-outline"), () -> {
            StandaloneFileBrowser.openSingleFile(() -> fileSystem.getValue(), fileStore -> {
                if (fileStore == null) {
                    filePath.setValue(null);
                    fileSystem.setValue(null);
                } else {
                    filePath.setValue(fileStore.getPath());
                    fileSystem.setValue(fileStore.getFileSystem());
                }
            });
        }).styleClass(Styles.CENTER_PILL).grow(false, true);


        var canGitShare = BindingsHelper.persist(Bindings.createBooleanBinding(() -> {
            if (!AppPrefs.get().enableGitStorage().get() || filePath.getValue() == null || ContextualFileReference.of(filePath.getValue())
                    .isInDataDirectory()) {
                return false;
            }

            return true;
        }, filePath, AppPrefs.get().enableGitStorage()));
        var gitShareButton = new ButtonComp(null, new FontIcon("mdi2g-git"), () -> {
            if (filePath.getValue() == null || filePath.getValue().isBlank() ||
                    !canGitShare.get()) {
                return;
            }

            try {
                var data = DataStorage.get().getDataDir();
                var f = data.resolve(FileNames.getFileName(filePath.getValue()));
                var source = Path.of(filePath.getValue());
                if (Files.exists(source)) {
                    var shouldCopy = AppWindowHelper.showBlockingAlert(alert -> {
                        alert.setTitle(AppI18n.get("confirmGitShareTitle"));
                        alert.setHeaderText(AppI18n.get("confirmGitShareHeader"));
                        alert.setAlertType(Alert.AlertType.CONFIRMATION);
                    }).map(buttonType -> buttonType.getButtonData().isDefaultButton()).orElse(false);
                    if (!shouldCopy) {
                        return;
                    }

                    Files.copy(source, f, StandardCopyOption.REPLACE_EXISTING);
                    Platform.runLater(() -> {
                        filePath.setValue(f.toString());
                    });
                }
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).handle();
            }
        });
        gitShareButton.apply(new FancyTooltipAugment<>("gitShareFileTooltip"));
        gitShareButton.styleClass(Styles.RIGHT_PILL).grow(false, true);

        var layout = new HorizontalComp(List.of(fileNameComp, fileBrowseButton, gitShareButton))
                .apply(struc -> struc.get().setFillHeight(true));

        layout.apply(struc -> {
            struc.get().focusedProperty().addListener((observable, oldValue, newValue) -> {
                struc.get().getChildren().getFirst().requestFocus();
            });
        });

        return layout.createRegion();
    }
}
