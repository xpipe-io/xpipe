package io.xpipe.app.prefs;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.util.PlatformThread;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.stream.Collectors;

public class AppPrefsComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var map = AppPrefs.get().getCategories().stream()
                .collect(Collectors.toMap(appPrefsCategory -> appPrefsCategory, appPrefsCategory -> {
                    var r = appPrefsCategory
                            .create()
                            .maxWidth(800)
                            .padding(new Insets(40, 40, 20, 60))
                            .styleClass("prefs-container")
                            .createRegion();
                    PlatformThread.runNestedLoopIteration();
                    return r;
                }));
        var pfxSp = new ScrollPane();
        AppPrefs.get().getSelectedCategory().subscribe(val -> {
            PlatformThread.runLaterIfNeeded(() -> {
                pfxSp.setContent(map.get(val));
            });
        });
        AppPrefs.get().getSelectedCategory().addListener((observable, oldValue, newValue) -> {
            pfxSp.setVvalue(0);
        });
        pfxSp.setFitToWidth(true);
        var pfxLimit = new StackPane(pfxSp);
        pfxLimit.setAlignment(Pos.TOP_LEFT);

        var sidebar = new AppPrefsSidebarComp().createRegion();
        sidebar.setMinWidth(260);
        sidebar.setPrefWidth(260);
        sidebar.setMaxWidth(260);

        var split = new HBox(sidebar, pfxLimit);
        HBox.setMargin(sidebar, new Insets(4));
        HBox.setHgrow(pfxLimit, Priority.ALWAYS);
        split.setFillHeight(true);
        split.getStyleClass().add("prefs");
        var stack = new StackPane(split);
        stack.setPickOnBounds(false);
        return stack;
    }
}
