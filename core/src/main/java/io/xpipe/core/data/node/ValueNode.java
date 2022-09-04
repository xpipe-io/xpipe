package io.xpipe.core.data.node;

import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.ValueType;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public abstract class ValueNode extends DataStructureNode {


    protected ValueNode() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ValueNode that)) {
            return false;
        }
        return Arrays.equals(getRawData(), that.getRawData()) && Objects.equals(getMetaAttributes(), that.getMetaAttributes());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getRawData());
    }

    public static ValueNode nullValue() {
        return new SimpleImmutableValueNode(new byte[0]);
    }

    public static ValueNode of(byte[] data) {
        return new SimpleImmutableValueNode(data);
    }

    public static ValueNode of(Object o) {
        return of(o.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public final int asInt() {
        return Integer.parseInt(asString());
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
