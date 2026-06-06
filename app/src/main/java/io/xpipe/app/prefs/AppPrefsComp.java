package io.xpipe.app.prefs;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.LeftSplitPaneComp;
import io.xpipe.app.comp.base.StackComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.util.BooleanScope;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import net.synedra.validatorfx.GraphicDecorationStackPane;

import java.util.List;

public class AppPrefsComp extends SimpleRegionBuilder {

    @Override
    protected Region createSimple() {
        var categories = AppPrefs.get().getCategories().stream()
                .filter(appPrefsCategory -> appPrefsCategory.show())
                .toList();
        var list = categories.stream()
                .map(appPrefsCategory -> {
                    var r = appPrefsCategory.create().style("prefs-container").style(appPrefsCategory.getId());
                    return r;
                })
                .toList();
        var boxComp = new VerticalComp(list);
        boxComp.apply(struc -> {
            struc.getStyleClass().add("prefs-box");
        });
        boxComp.maxWidth(950);
        var box = boxComp.build();

        var pane = new GraphicDecorationStackPane();
        pane.getChildren().add(box);

        var scrollPane = new ScrollPane(pane);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

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

        AppPrefs.get().getSelectedCategory().addListener((observable, oldValue, val) -> {
            if (val == null) {
                return;
            }

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

        var sidebar = new AppPrefsSidebarComp();
        var sidebarWrapper = new StackComp(List.of(sidebar));
        sidebarWrapper.padding(new Insets(4));
        sidebarWrapper.minWidth(265);
        sidebarWrapper.maxWidth(265);

        var split = new LeftSplitPaneComp(sidebarWrapper, RegionBuilder.of(() -> scrollPane));
        split.withInitialWidth(265);
        split.style("prefs");
        return split.build();
    }

    private double computeCategoryOffset(Region box, ScrollPane scrollPane, AppPrefsCategory val) {
        var node = val != null ? box.lookup("." + val.getId()) : null;
        if (node != null && scrollPane.getHeight() > 0.0) {
            var s = Math.min(
                            box.getHeight(),
                            node.getBoundsInParent().getMinY() > 0.0
                                    ? node.getBoundsInParent().getMinY() + 20
                                    : 0.0)
                    / box.getHeight();
            var off = (scrollPane.getHeight() * s * 1.02) / box.getHeight();
            return s + off;
        } else {
            return 0;
        }
    }
}
