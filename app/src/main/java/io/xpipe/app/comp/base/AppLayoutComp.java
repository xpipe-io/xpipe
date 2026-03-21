package io.xpipe.app.comp.base;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionStructure;
import io.xpipe.app.comp.RegionStructureBuilder;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.terminal.TerminalDockHubManager;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AppLayoutComp extends RegionStructureBuilder<BorderPane, AppLayoutComp.Structure> {

    @Override
    public Structure createBase() {
        var model = AppLayoutModel.get();
        Map<BaseRegionBuilder<?, ?>, ObservableValue<Boolean>> map = model.getEntries().stream()
                .filter(entry -> entry.comp() != null)
                .collect(Collectors.toMap(
                        entry -> entry.comp(),
                        entry -> Bindings.createBooleanBinding(
                                () -> {
                                    return model.getSelected().getValue().equals(entry);
                                },
                                model.getSelected()),
                        (v1, v2) -> v2,
                        LinkedHashMap::new));
        var multi = new MultiContentComp(true, map);
        multi.style("background");

        var pane = new BorderPane();
        var sidebar = new SideMenuBarComp(model.getSelected(), model.getEntries(), model.getQueueEntries());
        StackPane multiR = (StackPane) multi.build();
        pane.setCenter(multiR);
        var sidebarR = sidebar.build();
        pane.setRight(sidebarR);
        pane.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            sidebarR.getChildrenUnmodifiable().forEach(node -> {
                var shortcut = (KeyCodeCombination) node.getProperties().get("shortcut");
                if (shortcut != null && shortcut.match(event)) {
                    ((ButtonBase) ((Parent) node).getChildrenUnmodifiable().get(1)).fire();
                    event.consume();
                }
            });

            if (new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN).match(event)) {
                if (TerminalDockHubManager.get().getEnabled().get()) {
                    TerminalDockHubManager.get().toggleDock();
                    event.consume();
                }
            }
        });
        pane.getStyleClass().add("layout");
        return new Structure(pane, multiR, sidebarR, new ArrayList<>(multiR.getChildren()));
    }

    public record Structure(BorderPane pane, StackPane stack, Region sidebar, List<Node> children)
            implements RegionStructure<BorderPane> {

        public void prepareAddition() {
            stack.getChildren().clear();
            sidebar.setDisable(true);
        }

        public void show() {
            stack.getChildren().add(children.getFirst());
            for (int i = 1; i < children.size(); i++) {
                children.get(i).setVisible(false);
                children.get(i).setManaged(false);
                stack.getChildren().add(children.get(i));
            }
            PlatformThread.runNestedLoopIteration();
            sidebar.setDisable(false);
            stack.requestFocus();
        }

        @Override
        public BorderPane get() {
            return pane;
        }
    }
}
