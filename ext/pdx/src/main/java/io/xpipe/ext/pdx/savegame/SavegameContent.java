package io.xpipe.ext.pdx.savegame;

import io.xpipe.core.data.node.LinkedTupleNode;
import io.xpipe.core.data.node.TupleNode;

import java.util.Map;
import java.util.Set;

public final class SavegameContent {

    private final Map<String, TupleNode> nodes;

    public SavegameContent(Map<String, TupleNode> nodes) {
        this.nodes = nodes;
    }

    public TupleNode combinedNode() {
        return new LinkedTupleNode(nodes.values().stream().toList());
    }

    public Set<Map.Entry<String, TupleNode>> entrySet() {
        return nodes.entrySet();
    }

    public TupleNode get() {
        return combinedNode();
    }

    public TupleNode get(String name) {
        return nodes.get(name);
    }
}