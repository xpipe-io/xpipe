package io.xpipe.app.fxcomps.impl;

import atlantafx.base.theme.Styles;
import io.xpipe.app.browser.StandaloneFileBrowser;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.store.FileSystemStore;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class FileStoreChoiceComp extends SimpleComp {

    private final boolean onlyLocal;
    private final Property<FileStore> selected;

    public FileStoreChoiceComp(boolean onlyLocal, Property<FileStore> selected) {
        this.onlyLocal = onlyLocal;
        this.selected = selected;
    }

    private void setSelected(FileSystemStore fileSystem, String file) {
        selected.setValue(fileSystem != null && file != null ? new FileStore(fileSystem, file) : null);
    }

    @Override
    protected Region createSimple() {
        var fileProperty = new SimpleStringProperty(
                selected.getValue() != null ? selected.getValue().getFile() : null);
        fileProperty.addListener((observable, oldValue, newValue) -> {
            setSelected(selected.getValue().getFileSystem(), newValue);
        });
        selected.addListener((observable, oldValue, newValue) -> {
            fileProperty.setValue(newValue.getFile());
        });

        var fileSystemChoiceComp = new FileSystemStoreChoiceComp(selected).grow(false, true).styleClass(Styles.LEFT_PILL);
        if (onlyLocal) {
            fileSystemChoiceComp.hide(new SimpleBooleanProperty(true));
        }

        var fileNameComp = new TextFieldComp(fileProperty)
                .apply(struc -> HBox.setHgrow(struc.get(), Priority.ALWAYS))
                .styleClass(onlyLocal ? Styles.LEFT_PILL : Styles.CENTER_PILL);

        var fileBrowseButton = new ButtonComp(null, new FontIcon("mdi2f-folder-open-outline"), () -> {
                    StandaloneFileBrowser.openSingleFile(selected);
                })
                .styleClass(Styles.RIGHT_PILL).grow(false, true);

        var layout = new HorizontalComp(List.of(fileSystemChoiceComp, fileNameComp, fileBrowseButton)).apply(struc -> struc.get().setFillHeight(true));

        return layout.createRegion();
    }
}
