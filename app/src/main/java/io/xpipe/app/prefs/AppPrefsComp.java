package io.xpipe.app.prefs;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;

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
                    return appPrefsCategory
                            .create()
                            .maxWidth(700)
                            .padding(new Insets(40, 40, 20, 40))
                            .styleClass("prefs-container")
                            .createRegion();
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
        sidebar.setMinWidth(280);
        sidebar.setPrefWidth(280);
        sidebar.setMaxWidth(280);

        var split = new HBox(sidebar, pfxLimit);
        HBox.setHgrow(pfxLimit, Priority.ALWAYS);
        split.setFillHeight(true);
        split.getStyleClass().add("prefs");
        var stack = new StackPane(split);
        stack.setPickOnBounds(false);
        AppFont.medium(stack);
        return stack;
    }
}
