package io.xpipe.app.browser;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.FileReference;
import io.xpipe.core.store.FileSystemStore;
import javafx.beans.property.Property;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StandaloneFileBrowser {

    public static void localOpenFileChooser(
            Property<FileReference> fileStoreProperty, Window owner, Map<String, List<String>> extensions) {
        PlatformThread.runLaterIfNeeded(() -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(AppI18n.get("browseFileTitle"));
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(AppI18n.get("anyFile"), "*"));
            extensions.forEach((key, value) -> {
                fileChooser
                        .getExtensionFilters()
                        .add(new FileChooser.ExtensionFilter(
                                key, value.stream().map(v -> "*." + v).toArray(String[]::new)));
            });

            File file = fileChooser.showOpenDialog(owner);
            if (file != null && file.exists()) {
                fileStoreProperty.setValue(FileReference.local(file.toPath()));
            }
        });
    }

    public static void openSingleFile(Supplier<DataStoreEntryRef<? extends FileSystemStore>> store, Consumer<FileReference> file) {
        PlatformThread.runLaterIfNeeded(() -> {
            var model = new BrowserModel(BrowserModel.Mode.SINGLE_FILE_CHOOSER);
            var comp = new BrowserComp(model)
                    .apply(struc -> struc.get().setPrefSize(1200, 700))
                    .apply(struc -> AppFont.normal(struc.get()));
            var window = AppWindowHelper.sideWindow(AppI18n.get("openFileTitle"), stage -> comp, true, null);
            model.setOnFinish(fileStores -> {
                file.accept(fileStores.size() > 0 ? fileStores.get(0) : null);
                window.close();
            });
            window.show();
            model.openFileSystemAsync(store.get(), null, null);
        });
    }

    public static void saveSingleFile(Property<FileReference> file) {
        PlatformThread.runLaterIfNeeded(() -> {
            var model = new BrowserModel(BrowserModel.Mode.SINGLE_FILE_SAVE);
            var comp = new BrowserComp(model)
                    .apply(struc -> struc.get().setPrefSize(1200, 700))
                    .apply(struc -> AppFont.normal(struc.get()));
            var window = AppWindowHelper.sideWindow(AppI18n.get("saveFileTitle"), stage -> comp, true, null);
            model.setOnFinish(fileStores -> {
                file.setValue(fileStores.size() > 0 ? fileStores.get(0) : null);
                window.close();
            });
            window.show();
        });
    }
}
