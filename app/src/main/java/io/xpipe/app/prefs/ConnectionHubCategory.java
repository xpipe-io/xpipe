package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppResources;
import io.xpipe.app.util.FileOpener;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;

public class ConnectionHubCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "connectionHub";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2c-connection");
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        var connectionsBuilder = new OptionsBuilder()
                .nameAndDescription("connectionNotesTemplate")
                .addComp(new ButtonComp(AppI18n.observable("connectionNotesButton"),
                        new ReadOnlyObjectWrapper<>(new LabelGraphic.IconGraphic("mdi2a-application-edit-outline")), () -> {
                    editNotesTemplate();
                }), prefs.notesTemplate)
                .pref(prefs.condenseConnectionDisplay)
                .addToggle(prefs.condenseConnectionDisplay)
                .pref(prefs.showChildCategoriesInParentCategory)
                .addToggle(prefs.showChildCategoriesInParentCategory)
                .pref(prefs.openConnectionSearchWindowOnConnectionCreation)
                .addToggle(prefs.openConnectionSearchWindowOnConnectionCreation)
                .pref(prefs.requireDoubleClickForConnections)
                .addToggle(prefs.requireDoubleClickForConnections);
        var options = new OptionsBuilder().addTitle("connectionHub").sub(connectionsBuilder);
        return options.buildComp();
    }

    private static final UUID NOTES_UUID = UUID.randomUUID();

    private void editNotesTemplate() {
        AtomicReference<String> val = new AtomicReference<>(AppPrefs.get().notesTemplate.getValue());
        if (val.get() == null) {
            AppResources.with(AppResources.MAIN_MODULE, "misc/notes_default.md", f -> {
                val.set(Files.readString(f));
            });
        }

        FileOpener.openString("notes", NOTES_UUID, val.get(), s -> {
            AppPrefs.get().notesTemplate.set(s);
        });
    }
}
