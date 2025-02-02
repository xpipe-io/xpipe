package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AppLayoutComp extends Comp<AppLayoutComp.Structure> {

    private final AppLayoutModel model = AppLayoutModel.get();

    @Override
    public Structure createBase() {
        Map<Comp<?>, ObservableValue<Boolean>> map = model.getEntries().stream()
                .filter(entry -> entry.comp() != null)
                .collect(Collectors.toMap(
                        entry -> entry.comp(),
                        entry -> Bindings.createBooleanBinding(
                                () -> {
                                    return model.getSelected().getValue().equals(entry);
                                },
                                model.getSelected())));
        var multi = new MultiContentComp(map);
        multi.styleClass("background");

        var pane = new BorderPane();
        var sidebar = new SideMenuBarComp(model.getSelected(), model.getEntries());
        StackPane multiR = (StackPane) multi.createRegion();
        pane.setCenter(multiR);
        var sidebarR = sidebar.createRegion();
        pane.setRight(sidebarR);
        model.getSelected().addListener((c, o, n) -> {
            if (o != null && o.equals(model.getEntries().get(2))) {
                AppPrefs.get().save();
                DataStorage.get().saveAsync();
            }

            if (o != null && o.equals(model.getEntries().get(0))) {
                StoreViewState.get().updateDisplay();
            }
        });
        pane.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            sidebarR.getChildrenUnmodifiable().forEach(node -> {
                var shortcut = (KeyCodeCombination) node.getProperties().get("shortcut");
                if (shortcut != null && shortcut.match(event)) {
                    ((ButtonBase) ((Parent) node).getChildrenUnmodifiable().get(1)).fire();
                    event.consume();
                }
            });
        });
        AppFontSizes.sm(pane);
        pane.getStyleClass().add("layout");
        return new Structure(pane, multiR, sidebarR, new ArrayList<>(multiR.getChildren()));
    }

    public record Structure(BorderPane pane, StackPane stack, Region sidebar, List<Node> children)
            implements CompStructure<BorderPane> {

        public void prepareAddition() {
            stack.getChildren().clear();
            sidebar.setDisable(true);
        }

        public void show() {
            for (var child : children) {
                stack.getChildren().add(child);
                PlatformThread.runNestedLoopIteration();
            }
            sidebar.setDisable(false);
        }

        @Override
        public BorderPane get() {
            return pane;
        }
    }
}
