package io.xpipe.app.comp.storage.store;

import atlantafx.base.theme.Styles;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class StoreSidebarComp extends SimpleComp {
    @Override
    protected Region createSimple() {
        var sideBar = new VerticalComp(List.of(
                new StoreEntryListHeaderComp().styleClass(Styles.ELEVATED_1),
                new StoreCreationBarComp().styleClass(Styles.ELEVATED_1),
                Comp.of(() -> new Region()).styleClass("bar").styleClass("filler-bar")));
        sideBar.apply(s -> VBox.setVgrow(s.get().getChildren().get(2), Priority.ALWAYS));
        sideBar.styleClass("sidebar");
        return sideBar.createRegion();
    }
}
