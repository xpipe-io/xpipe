package io.xpipe.app.prefs;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.LeftSplitPaneComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.base.StackComp;
import io.xpipe.app.comp.base.VerticalComp;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.util.BooleanScope;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
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
        var boxComp = new ListBoxViewComp<>(FXCollections.observableArrayList(categories),
                FXCollections.observableArrayList(categories), appPrefsCategory -> {
            var r = appPrefsCategory
                    .create()
                    .style("prefs-container")
                    .style(appPrefsCategory.getId());
            return r;
        }, true);
        boxComp.setVisibilityControl(true);
        boxComp.apply(struc -> {
            struc.getContent().getStyleClass().add("prefs-box");
        });
        boxComp.maxWidth(1050);
        var box = boxComp.build();

        var pane = new GraphicDecorationStackPane();
        pane.getChildren().add(box);

        var externalUpdate = new SimpleBooleanProperty();

        boxComp.apply(scrollPane -> {
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
        });

        var sidebar = new AppPrefsSidebarComp();
        var sidebarWrapper = new StackComp(List.of(sidebar));
        sidebarWrapper.padding(new Insets(4));
        sidebarWrapper.minWidth(265);
        sidebarWrapper.maxWidth(265);

        var split = new LeftSplitPaneComp(sidebarWrapper, new StackComp(List.of(boxComp)).padding(new Insets(4, 0, 0, 0)));
        split.withInitialWidth(265);
        split.style("prefs");
        return split.build();
    }

    private double computeCategoryOffset(Region box, ScrollPane scrollPane, AppPrefsCategory val) {
        var node = val != null ? box.lookup("." + val.getId()) : null;
        if (node != null && scrollPane.getHeight() > 0.0) {
            var minY = node.getBoundsInParent().getMinY();
            if (minY <= 40.0) {
                minY = 0.0;
            }
            var s = Math.min(box.getHeight(), minY > 0.0 ? minY + 20 : 0.0) / box.getHeight();
            var off = (scrollPane.getHeight() * s * 1.02) / box.getHeight();
            return s + off;
        } else {
            return 0;
        }
    }
}
