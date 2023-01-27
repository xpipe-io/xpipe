package io.xpipe.ext.pdx.parser;

import io.xpipe.core.data.node.ValueNode;

import java.nio.charset.StandardCharsets;

public class ContextValueNode extends ValueNode {

    private final NodeContext context;
    private final int scalarIndex;

    public ContextValueNode(NodeContext context, int scalarIndex) {
        this.context = context;
        this.scalarIndex = scalarIndex;
    }

    @Override
    public final String asString() {
        var s = context.evaluate(scalarIndex);
        if (isTextual()) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    private boolean isTextual() {
        return context.isQuoted(scalarIndex);
    }

    @Override
    public byte[] getRawData() {
        return asString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toString(int indent) {
        return (isTextual() ? "\"" : "") + asString() + (isTextual() ? "\"" : "") + " (I)";
    }

    @Override
    public boolean isMutable() {
        return false;
    }
}
