package io.xpipe.app.comp;

import io.xpipe.app.comp.base.MultiContentComp;
import io.xpipe.app.comp.base.SideMenuBarComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import javafx.beans.binding.Bindings;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import java.util.stream.Collectors;

public class AppLayoutComp extends Comp<CompStructure<Pane>> {

    private final AppLayoutModel model = AppLayoutModel.get();

    @Override
    public CompStructure<Pane> createBase() {
        var multi = new MultiContentComp(model.getEntries().stream()
                .collect(Collectors.toMap(
                        entry -> entry.comp(),
                        entry -> PlatformThread.sync(Bindings.createBooleanBinding(
                                () -> {
                                    return model.getSelected().getValue().equals(entry);
                                },
                                model.getSelected())))));

        var pane = new BorderPane();
        var sidebar = new SideMenuBarComp(model.getSelected(), model.getEntries());
        pane.setCenter(multi.createRegion());
        pane.setRight(sidebar.createRegion());
        pane.getStyleClass().add("background");
        model.getSelected().addListener((c, o, n) -> {
            if (o != null && o.equals(model.getEntries().get(2))) {
                AppPrefs.get().save();
            }
        });
        AppFont.normal(pane);
        return new SimpleCompStructure<>(pane);
    }
}
