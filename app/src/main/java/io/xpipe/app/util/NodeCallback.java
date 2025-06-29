package io.xpipe.app.util;

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

public class NodeCallback {

    private static final Set<Window> windows = new HashSet<>();
    private static final Set<Node> nodes = new HashSet<>();

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

    @SuppressWarnings("unchecked")
    private static void watchPlatformThreadChanges(Node node) {
        watchGraph(node, c -> {
            if (!nodes.add(c)) {
                return;
            }

            if (c instanceof Parent p) {
                p.getChildrenUnmodifiable().addListener(listListener);
            }

            c.visibleProperty().addListener(listener);
            c.boundsInParentProperty().addListener(listener);
            c.managedProperty().addListener(listener);
            c.opacityProperty().addListener(listener);
            c.accessibleHelpProperty().addListener(listener);
            c.accessibleTextProperty().addListener(listener);
        });
    }

    private static void watchGraph(Node node, Consumer<Node> callback) {
        if (node instanceof Parent p) {
            for (Node c : p.getChildrenUnmodifiable()) {
                watchGraph(c, callback);
            }
            p.getChildrenUnmodifiable().addListener((ListChangeListener<? super Node>) change -> {
                for (Node c : change.getList()) {
                    watchGraph(c, callback);
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
