package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStorage;

import javafx.beans.property.ReadOnlyObjectWrapper;

import java.util.List;

public class FileBrowserCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "fileBrowser";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2f-file-cabinet");
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        return new OptionsBuilder()
                .addTitle("fileBrowser")
                .sub(new OptionsBuilder()
                        .pref(prefs.enableTerminalDocking)
                        .addToggle(prefs.enableTerminalDocking)
                        .pref(prefs.editFilesWithDoubleClick)
                        .addToggle(prefs.editFilesWithDoubleClick)
                        .pref(prefs.downloadsDirectory)
                        .addComp(
                                new ContextualFileReferenceChoiceComp(
                                                new ReadOnlyObjectWrapper<>(DataStorage.get()
                                                        .local()
                                                        .ref()),
                                                prefs.downloadsDirectory,
                                                null,
                                                List.of(),
                                                e -> e.equals(DataStorage.get().local()),
                                                true)
                                        .maxWidth(getCompWidth()),
                                prefs.downloadsDirectory)
                        .pref(prefs.pinLocalMachineOnStartup)
                        .addToggle(prefs.pinLocalMachineOnStartup))
                .buildComp();
    }
}
