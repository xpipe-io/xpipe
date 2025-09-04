package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.OsType;

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

import org.apache.commons.lang3.SystemUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AppLayoutComp extends Comp<AppLayoutComp.Structure> {

    @Override
    public Structure createBase() {
        var model = AppLayoutModel.get();
        Map<Comp<?>, ObservableValue<Boolean>> map = model.getEntries().stream()
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
        var multi = new MultiContentComp(map, true);
        multi.styleClass("background");

        multi.apply(struc -> {
            struc.get()
                    .opacityProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                // Only Windows 11 has colored background support
                                if (OsType.getLocal() == OsType.WINDOWS && !SystemUtils.IS_OS_WINDOWS_11) {
                                    return 1.0;
                                }

                                if (OsType.getLocal() == OsType.LINUX) {
                                    return 1.0;
                                }

                                // On macOS, we don't have a transparent background in dev mode
                                if (OsType.getLocal() == OsType.MACOS
                                        && AppProperties.get().isDevelopmentEnvironment()) {
                                    return 1.0;
                                }

                                return AppPrefs.get().performanceMode().get() ? 1.0 : 0.95;
                            },
                            AppPrefs.get().performanceMode()));
        });

        var pane = new BorderPane();
        var sidebar = new SideMenuBarComp(model.getSelected(), model.getEntries(), model.getQueueEntries());
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
                StoreViewState.get().triggerStoreListUpdate();
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
            stack.getChildren().add(children.getFirst());
            for (int i = 1; i < children.size(); i++) {
                children.get(i).setVisible(false);
                children.get(i).setManaged(false);
                stack.getChildren().add(children.get(i));
            }
            PlatformThread.runNestedLoopIteration();
            sidebar.setDisable(false);
        }

        @Override
        public BorderPane get() {
            return pane;
        }
    }
}
