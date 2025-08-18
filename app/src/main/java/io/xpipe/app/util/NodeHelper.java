package io.xpipe.app.util;

import javafx.scene.Node;

import java.util.Set;

public class NodeHelper {

    public static boolean isParent(Set<Node> parent, Object child) {
        return parent.stream().anyMatch(node -> isParent(node, child));
    }

    public static boolean isParent(Node parent, Object child) {
        if (child == null) {
            return false;
        }

        if (!(child instanceof Node n)) {
            return false;
        }

        var c = n.getParent();
        while (c != null) {
            if (c == parent) {
                return true;
            }

            c = c.getParent();
        }
        return false;
    }
}
