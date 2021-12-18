package io.xpipe.core.data.node;

import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.ValueType;
import lombok.EqualsAndHashCode;

import java.nio.charset.StandardCharsets;

@EqualsAndHashCode(callSuper = false)
public abstract class ValueNode extends DataStructureNode {

    private static final byte[] NULL = new byte[] {0};

    public static ValueNode immutable(byte[] data) {
        return new ImmutableValueNode(data);
    }

    public static ValueNode mutableNull() {
        return mutable(NULL);
    }

    public static ValueNode nullValue() {
        return mutable(NULL);
    }

    public static ValueNode mutable(byte[] data) {
        return new MutableValueNode(data);
    }

    public static ValueNode mutable(Object o) {
        return mutable(o.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static ValueNode of(byte[] data) {
        return mutable(data);
    }

    public static ValueNode of(Object o) {
        return mutable(o);
    }

    protected ValueNode() {
    }

    @Override
    public abstract DataStructureNode setRawData(byte[] data);

    @Override
    public final int asInt() {
        return Integer.parseInt(asString());
    }

    @Override
    public final String asString() {
        return new String(getRawData());
    }

    @Override
    public final boolean isValue() {
        return true;
    }

    @Override
    protected final String getName() {
        return "value node";
    }

    @Override
    public final DataType determineDataType() {
        return ValueType.of();
    }

    public abstract byte[] getRawData();
}
