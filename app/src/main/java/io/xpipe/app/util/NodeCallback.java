package io.xpipe.app.util;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.util.function.Consumer;

public class NodeCallback {

    public static void watchGraph(Node node, Consumer<Node> callback) {
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

    public static void watchPlatformThreadChanges(Node node) {
        watchGraph(node, c -> {
            if (c instanceof Parent p) {
                p.getChildrenUnmodifiable().addListener((ListChangeListener<? super Node>) change -> {
                    checkPlatformThread();
                });
            }
            c.visibleProperty().addListener((observable, oldValue, newValue) -> {
                checkPlatformThread();
            });
            c.boundsInParentProperty().addListener((observable, oldValue, newValue) -> {
                checkPlatformThread();
            });
            c.managedProperty().addListener((observable, oldValue, newValue) -> {
                checkPlatformThread();
            });
            c.opacityProperty().addListener((observable, oldValue, newValue) -> {
                checkPlatformThread();
            });
        });
    }

    private static void checkPlatformThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Not in Fx application thread");
        }
    }
}
