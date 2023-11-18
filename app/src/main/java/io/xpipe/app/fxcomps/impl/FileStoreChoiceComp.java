package io.xpipe.app.fxcomps.impl;

import atlantafx.base.theme.Styles;
import io.xpipe.app.browser.StandaloneFileBrowser;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.core.store.FileSystemStore;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class FileStoreChoiceComp extends SimpleComp {

    private final boolean hideFileSystem;
    private final Property<FileSystemStore> fileSystem;
    private final Property<String> filePath;

    public FileStoreChoiceComp(boolean hideFileSystem, Property<FileSystemStore> fileSystem, Property<String> filePath) {
        this.hideFileSystem = hideFileSystem;
        this.fileSystem = fileSystem != null ? fileSystem : new SimpleObjectProperty<>();
        this.filePath = filePath;
    }

    @Override
    protected Region createSimple() {
        var fileSystemChoiceComp =
                new FileSystemStoreChoiceComp(fileSystem).grow(false, true).styleClass(Styles.LEFT_PILL);
        if (hideFileSystem) {
            fileSystemChoiceComp.hide(new SimpleBooleanProperty(true));
        }

        var fileNameComp = new TextFieldComp(filePath)
                .apply(struc -> HBox.setHgrow(struc.get(), Priority.ALWAYS))
                .styleClass(hideFileSystem ? Styles.LEFT_PILL : Styles.CENTER_PILL)
                .grow(false, true);

        var fileBrowseButton = new ButtonComp(null, new FontIcon("mdi2f-folder-open-outline"), () -> {
                    StandaloneFileBrowser.openSingleFile(() -> null, fileStore -> {
                        if (fileStore == null) {
                            filePath.setValue(null);
                            fileSystem.setValue(null);
                        } else {
                            filePath.setValue(fileStore.getPath());
                            fileSystem.setValue(fileStore.getFileSystem());
                        }
                    });
                })
                .styleClass(Styles.RIGHT_PILL)
                .grow(false, true);

        var layout = new HorizontalComp(List.of(fileSystemChoiceComp, fileNameComp, fileBrowseButton))
                .apply(struc -> struc.get().setFillHeight(true));

        layout.apply(struc -> {
            struc.get().focusedProperty().addListener((observable, oldValue, newValue) -> {
                    struc.get().getChildren().get(1).requestFocus();
            });
        });

        return layout.createRegion();
    }
}
