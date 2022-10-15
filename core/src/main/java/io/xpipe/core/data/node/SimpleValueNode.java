package io.xpipe.core.data.node;

import lombok.NonNull;

import java.nio.charset.StandardCharsets;

public class SimpleValueNode extends ValueNode {

    private final byte @NonNull [] data;

    SimpleValueNode(byte @NonNull [] data) {
        this.data = data;
    }

    public byte[] getRawData() {
        return data;
    }

    @Override
    public final String asString() {
        return new String(getRawData(), StandardCharsets.UTF_8);
    }

    @Override
    public String toString(int indent) {
        var string = getRawData().length == 0 && !hasMetaAttribute(IS_TEXT)
                ? "<null>"
                : new String(getRawData(), StandardCharsets.UTF_8);
        return (hasMetaAttribute(IS_TEXT) ? "\"" : "") + string + (hasMetaAttribute(IS_TEXT) ? "\"" : "") + " "
                + metaToString();
    }

    @Override
    public boolean isMutable() {
        return false;
    }
}
