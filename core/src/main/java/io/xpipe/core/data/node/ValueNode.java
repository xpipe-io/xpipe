package io.xpipe.core.data.node;

import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.ValueType;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public abstract class ValueNode extends DataStructureNode {

    private static final byte[] NULL = new byte[]{0};

    protected ValueNode() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ValueNode that)) return false;
        return Arrays.equals(getRawData(), that.getRawData());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getRawData());
    }

    @Override
    public abstract ValueNode immutableView();

    @Override
    public abstract ValueNode mutableCopy();

    public static ValueNode immutable(byte[] data) {
        return new ImmutableValueNode(data);
    }

    public static ValueNode immutable(Object o) {
        return immutable(o.toString().getBytes(StandardCharsets.UTF_8));
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
