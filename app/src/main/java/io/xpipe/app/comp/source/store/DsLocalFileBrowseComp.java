package io.xpipe.app.comp.source.store;

import io.xpipe.app.browser.StandaloneFileBrowser;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.util.JfxHelper;
import io.xpipe.core.impl.FileStore;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;

import java.util.concurrent.atomic.AtomicReference;

@AllArgsConstructor
public class DsLocalFileBrowseComp extends Comp<CompStructure<Button>> {

    private final ObservableValue<DataSourceProvider<?>> provider;
    private final Property<FileStore> chosenFile;
    private final DsStreamStoreChoiceComp.Mode mode;

    @Override
    public CompStructure<Button> createBase() {
        var button = new AtomicReference<Button>();
        button.set(new ButtonComp(null, getGraphic(), () -> {
                    if (mode == DsStreamStoreChoiceComp.Mode.OPEN) {
                        StandaloneFileBrowser.openSingleFile(chosenFile);
                    } else {
                        StandaloneFileBrowser.saveSingleFile(chosenFile);
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

    private Region getGraphic() {
        var graphic = hasProvider() ? provider.getValue().getDisplayIconFileName() : "file_icon.png";
        if (chosenFile.getValue() == null || !(chosenFile.getValue() instanceof FileStore f) || f.getPath() == null) {
            return JfxHelper.createNamedEntry(AppI18n.get("browse"), AppI18n.get("selectFileFromComputer"), graphic);
        } else {
            return JfxHelper.createNamedEntry(f.getFileName(), f.getPath(), graphic);
        }
    }
}
