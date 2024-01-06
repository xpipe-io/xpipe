package io.xpipe.app.fxcomps.impl;

import atlantafx.base.theme.Styles;
import io.xpipe.app.browser.StandaloneFileBrowser;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.FileSystemStore;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class ContextualFileReferenceChoiceComp extends SimpleComp {

    private final Property<DataStoreEntryRef<? extends FileSystemStore>> fileSystem;
    private final Property<String> filePath;

    public <T extends FileSystemStore> ContextualFileReferenceChoiceComp(ObservableValue<DataStoreEntryRef<T>> fileSystem, Property<String> filePath) {
        this.fileSystem = new SimpleObjectProperty<>();
        SimpleChangeListener.apply(fileSystem, val -> {
            this.fileSystem.setValue(val);
        });
        this.filePath = filePath;
    }

    @Override
    protected Region createSimple() {
        var fileNameComp = new TextFieldComp(filePath)
                .apply(struc -> HBox.setHgrow(struc.get(), Priority.ALWAYS))
                .styleClass(Styles.LEFT_PILL)
                .grow(false, true);

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
                })
                .styleClass(Styles.RIGHT_PILL)
                .grow(false, true);

        var layout = new HorizontalComp(List.of(fileNameComp, fileBrowseButton))
                .apply(struc -> struc.get().setFillHeight(true));

        layout.apply(struc -> {
            struc.get().focusedProperty().addListener((observable, oldValue, newValue) -> {
                    struc.get().getChildren().get(1).requestFocus();
            });
        });

        return layout.createRegion();
    }
}
