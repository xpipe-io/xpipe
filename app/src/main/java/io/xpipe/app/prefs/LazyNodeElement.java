package io.xpipe.app.prefs;

import com.dlsc.formsfx.model.structure.Element;
import javafx.scene.Node;

import java.util.function.Supplier;

public class LazyNodeElement<N extends Node> extends Element<LazyNodeElement<N>> {

    protected Supplier<N> node;

    protected LazyNodeElement(Supplier<N> node) {
        if (node == null) {
            throw new NullPointerException("Node argument must not be null");
        }
        this.node = node;
    }

    public static <T extends Node> LazyNodeElement<T> of(Supplier<T> node) {
        return new LazyNodeElement<>(node);
    }

    public N getNode() {
        return node.get();
    }
}