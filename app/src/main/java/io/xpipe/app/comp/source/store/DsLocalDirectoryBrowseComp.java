package io.xpipe.app.comp.source.store;

import com.jfoenix.controls.JFXButton;
import io.xpipe.app.util.JfxHelper;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import javafx.beans.property.Property;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Path;

public class DsLocalDirectoryBrowseComp extends Comp<CompStructure<Button>> {

    private final DataSourceProvider provider;
    private final Property<Path> chosenDir;

    public DsLocalDirectoryBrowseComp(DataSourceProvider provider, Property<Path> chosenDir) {
        this.provider = provider;
        this.chosenDir = chosenDir;
    }

    @Override
    public CompStructure<Button> createBase() {
        var button = new JFXButton();
        button.setGraphic(getGraphic());
        button.setOnAction(e -> {
            var dirChooser = new DirectoryChooser();
            dirChooser.setTitle(
                    I18n.get("browseDirectoryTitle", provider.getFileProvider().getFileName()));
            File file = dirChooser.showDialog(button.getScene().getWindow());
            if (file != null && file.exists()) {
                chosenDir.setValue(file.toPath());
            }
            e.consume();
        });

        chosenDir.addListener((c, o, n) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                var newGraphic = getGraphic();
                button.setGraphic(newGraphic);
                button.layout();
            });
        });

        return new SimpleCompStructure<>(button);
    }

    private Region getGraphic() {
        var graphic = provider.getDisplayIconFileName();
        if (chosenDir.getValue() == null) {
            return JfxHelper.createNamedEntry(I18n.get("browse"), I18n.get("selectDirectoryFromComputer"), graphic);
        } else {
            return JfxHelper.createNamedEntry(
                    chosenDir.getValue().getFileName().toString(),
                    chosenDir.getValue().toString(),
                    graphic);
        }
    }
}
