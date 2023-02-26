package io.xpipe.app.browser;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.core.impl.FileStore;
import javafx.beans.property.Property;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;
import java.util.Map;

public class StandaloneFileBrowser {

    public static void localOpenFileChooser(Property<FileStore> fileStoreProperty, Window owner, Map<String, List<String>> extensions) {
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
            fileStoreProperty.setValue(FileStore.local(file.toPath()));
        }
    }

    public static void openSingleFile(Property<FileStore> file) {
        var model = new FileBrowserModel(FileBrowserModel.Mode.SINGLE_FILE_CHOOSER);
        var comp = new FileBrowserComp(model)
                .apply(struc -> struc.get().setPrefSize(1200, 700))
                .apply(struc -> AppFont.normal(struc.get()));
        var window = AppWindowHelper.sideWindow(AppI18n.get("openFileTitle"), stage -> comp, true, null);
        model.setOnFinish(fileStores -> {
            file.setValue(fileStores.size() > 0 ? fileStores.get(0) : null);
            window.close();
        });
        window.show();
    }

    public static void saveSingleFile(Property<FileStore> file) {
        var model = new FileBrowserModel(FileBrowserModel.Mode.SINGLE_FILE_SAVE);
        var comp = new FileBrowserComp(model)
                .apply(struc -> struc.get().setPrefSize(1200, 700))
                .apply(struc -> AppFont.normal(struc.get()));
        var window = AppWindowHelper.sideWindow(AppI18n.get("saveFileTitle"), stage -> comp, true, null);
        model.setOnFinish(fileStores -> {
            file.setValue(fileStores.size() > 0 ? fileStores.get(0) : null);
            window.close();
        });
        window.show();
    }
}
