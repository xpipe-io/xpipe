package io.xpipe.core.data.node;

import lombok.NonNull;

import java.nio.charset.StandardCharsets;

public class SimpleImmutableValueNode extends ValueNode {

    private final byte @NonNull [] data;

    SimpleImmutableValueNode(byte @NonNull [] data) {
        this.data = data;
    }

    public byte[] getRawData() {
        return data;
    }

    @Override
    public final String asString() {
        if (getRawData().length == 0 && !hasMetaAttribute(IS_TEXT)) {
            return "null";
        }

        return new String(getRawData(), StandardCharsets.UTF_8);
    }

    @Override
    public String toString(int indent) {
        var string = getRawData().length == 0 && !hasMetaAttribute(IS_TEXT) ? "<null>" : new String(getRawData(), StandardCharsets.UTF_8);
        return (hasMetaAttribute(IS_TEXT) ? "\"" : "") + string + (hasMetaAttribute(IS_TEXT) ? "\"" : "") + " " + metaToString();
    }

    @Override
    public boolean isMutable() {
        return false;
    }
}
