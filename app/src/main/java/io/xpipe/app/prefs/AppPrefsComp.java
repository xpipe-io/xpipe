package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

public class AppPrefsComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var categories = AppPrefs.get().getCategories().stream()
                .filter(appPrefsCategory -> appPrefsCategory.show())
                .toList();
        var list = categories.stream()
                .<Comp<?>>map(appPrefsCategory -> {
                    var r = appPrefsCategory
                            .create()
                            .styleClass("prefs-container")
                            .styleClass(appPrefsCategory.getId());
                    return r;
                })
                .toList();
        var box = new VerticalComp(list)
                .maxWidth(850)
                .styleClass("prefs-box")
                .createStructure()
                .get();
        var scrollPane = new ScrollPane(box);

        var externalUpdate = new SimpleBooleanProperty();

        scrollPane.vvalueProperty().addListener((observable, oldValue, newValue) -> {
            if (externalUpdate.get()) {
                return;
            }

            BooleanScope.executeExclusive(externalUpdate, () -> {
                var offset = newValue.doubleValue();
                if (offset == 1.0) {
                    AppPrefs.get().getSelectedCategory().setValue(categories.getLast());
                    return;
                }

                for (int i = categories.size() - 1; i >= 0; i--) {
                    var category = categories.get(i);
                    var min = computeCategoryOffset(box, scrollPane, category);
                    if (offset + (100.0 / box.getHeight()) > min) {
                        AppPrefs.get().getSelectedCategory().setValue(category);
                        return;
                    }
                }
            });
        });

        AppPrefs.get().getSelectedCategory().subscribe(val -> {
            PlatformThread.runLaterIfNeeded(() -> {
                if (externalUpdate.get()) {
                    return;
                }

                BooleanScope.executeExclusive(externalUpdate, () -> {
                    // This value is off initially if we haven't opened the settings before
                    // Perhaps it's the layout that is not done yet?
                    var off = computeCategoryOffset(box, scrollPane, val);
                    scrollPane.setVvalue(off);
                });
            });
        });
        scrollPane.setFitToWidth(true);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        var sidebar = new AppPrefsSidebarComp().createRegion();
        sidebar.setMinWidth(260);
        sidebar.setPrefWidth(260);
        sidebar.setMaxWidth(260);

        var split = new HBox(sidebar, scrollPane);
        HBox.setMargin(sidebar, new Insets(4));
        split.setFillHeight(true);
        split.getStyleClass().add("prefs");
        var stack = new StackPane(split);
        return stack;
    }

    private double computeCategoryOffset(VBox box, ScrollPane scrollPane, AppPrefsCategory val) {
        var node = val != null ? box.lookup("." + val.getId()) : null;
        if (node != null && scrollPane.getHeight() > 0.0) {
            var s = Math.min(
                            box.getHeight(),
                            node.getBoundsInParent().getMinY() > 0.0
                                    ? node.getBoundsInParent().getMinY() + 20
                                    : 0.0)
                    / box.getHeight();
            var off = (scrollPane.getHeight() * s * 1.05) / box.getHeight();
            return s + off;
        } else {
            return 0;
        }
    }
}
