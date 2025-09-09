package io.xpipe.app.platform;

import io.xpipe.app.core.AppProperties;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Window;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class PlatformThreadWatcher {

    private static final Set<Window> windows = new HashSet<>();
    private static final Set<Node> nodes = new HashSet<>();
    // Reuse listener for everything. Disabling generics allows that
    @SuppressWarnings("rawtypes")
    private static final ChangeListener listener = new ChangeListener() {

        @Override
        public void changed(ObservableValue observableValue, Object o, Object t1) {
            checkPlatformThread();
        }
    };

    @SuppressWarnings("rawtypes")
    private static final ListChangeListener listListener = new ListChangeListener() {

        @Override
        public void onChanged(Change change) {
            checkPlatformThread();
        }
    };

    public static void init() {
        if (!AppProperties.get().isDebugPlatformThreadAccess()) {
            return;
        }

        Window.getWindows().addListener((ListChangeListener<? super Window>) change -> {
            for (Window window : change.getList()) {
                if (!windows.add(window)) {
                    continue;
                }

                window.sceneProperty().subscribe(scene -> {
                    if (scene == null) {
                        return;
                    }

                    scene.rootProperty().subscribe(root -> {
                        if (root != null) {
                            watchPlatformThreadChanges(root);
                        }
                    });
                });
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static void watchPlatformThreadChanges(Node node) {
        watchGraph(node, c -> {
            c.sceneProperty().subscribe((oldScene, newScene) -> {
                var add = oldScene == null && newScene != null;
                var remove = oldScene != null && newScene == null;
                if (!add && !remove) {
                    return;
                }

                if (add && !nodes.add(c)) {
                    return;
                }

                if (remove) {
                    nodes.remove(c);
                }

                if (c instanceof Parent p) {
                    if (add) {
                        p.getChildrenUnmodifiable().addListener(listListener);
                    } else {
                        p.getChildrenUnmodifiable().removeListener(listListener);
                    }
                }

                if (add) {
                    c.visibleProperty().addListener(listener);
                    c.boundsInParentProperty().addListener(listener);
                    c.managedProperty().addListener(listener);
                    c.opacityProperty().addListener(listener);
                    c.accessibleHelpProperty().addListener(listener);
                    c.accessibleTextProperty().addListener(listener);
                } else {
                    c.visibleProperty().removeListener(listener);
                    c.boundsInParentProperty().removeListener(listener);
                    c.managedProperty().removeListener(listener);
                    c.opacityProperty().removeListener(listener);
                    c.accessibleHelpProperty().removeListener(listener);
                    c.accessibleTextProperty().removeListener(listener);
                }
            });
        });
    }

    private static void watchGraph(Node node, Consumer<Node> callback) {
        if (node instanceof Parent p) {
            for (Node c : p.getChildrenUnmodifiable()) {
                watchGraph(c, callback);
            }

            ListChangeListener<? super Node> childListener = change -> {
                for (Node c : change.getList()) {
                    watchGraph(c, callback);
                }
            };
            p.sceneProperty().subscribe(scene -> {
                if (scene != null) {
                    p.getChildrenUnmodifiable().addListener(childListener);
                } else {
                    p.getChildrenUnmodifiable().removeListener(childListener);
                }
            });
        }
        callback.accept(node);
    }

    private static void checkPlatformThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Not in Fx application thread");
        }
    }
}
