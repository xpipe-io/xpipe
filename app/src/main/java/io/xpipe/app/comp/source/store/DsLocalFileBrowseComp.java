package io.xpipe.app.comp.source.store;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.util.JfxHelper;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import lombok.AllArgsConstructor;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

@AllArgsConstructor
public class DsLocalFileBrowseComp extends Comp<CompStructure<Button>> {

    private final ObservableValue<DataSourceProvider<?>> provider;
    private final Property<DataStore> chosenFile;
    private final DsStreamStoreChoiceComp.Mode mode;

    @Override
    public CompStructure<Button> createBase() {
        var button = new AtomicReference<Button>();
        button.set(new ButtonComp(null, getGraphic(), () -> {
                    var fileChooser = createChooser();
                    File file = mode == DsStreamStoreChoiceComp.Mode.OPEN
                            ? fileChooser.showOpenDialog(button.get().getScene().getWindow())
                            : fileChooser.showSaveDialog(button.get().getScene().getWindow());
                    if (file != null && file.exists()) {
                        chosenFile.setValue(FileStore.local(file.toPath()));
                    }
                })
                .createStructure()
                .get());

        Runnable update = () -> {
            PlatformThread.runLaterIfNeeded(() -> {
                var newGraphic = getGraphic();
                button.get().setGraphic(newGraphic);
                button.get().layout();
            });
        };

        chosenFile.addListener((c, o, n) -> {
            update.run();
        });

        if (provider != null) {
            provider.addListener((c, o, n) -> {
                update.run();
            });
        }

        return new SimpleCompStructure<>(button.get());
    }

    private boolean hasProvider() {
        return provider != null && provider.getValue() != null;
    }

    private FileChooser createChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(AppI18n.get("browseFileTitle"));

        if (!hasProvider()) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(AppI18n.get("anyFile"), "*"));
            return fileChooser;
        }

        if (hasProvider()) {
            provider.getValue().getFileProvider().getFileExtensions().forEach((key, value) -> {
                var name = AppI18n.get(key);
                if (value != null) {
                    fileChooser
                            .getExtensionFilters()
                            .add(new FileChooser.ExtensionFilter(
                                    name, value.stream().map(v -> "*." + v).toArray(String[]::new)));
                } else {
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(name, "*"));
                }
            });

            if (!provider.getValue().getFileProvider().getFileExtensions().containsValue(null)) {
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(AppI18n.get("anyFile"), "*"));
            }
        }

        return fileChooser;
    }

    private Region getGraphic() {
        var graphic = hasProvider() ? provider.getValue().getDisplayIconFileName() : "file_icon.png";
        if (chosenFile.getValue() == null || !(chosenFile.getValue() instanceof FileStore f) || f.getFile() == null) {
            return JfxHelper.createNamedEntry(AppI18n.get("browse"), AppI18n.get("selectFileFromComputer"), graphic);
        } else {
            return JfxHelper.createNamedEntry(
                    f.getFileName().toString(), f.getFile().toString(), graphic);
        }
    }
}
