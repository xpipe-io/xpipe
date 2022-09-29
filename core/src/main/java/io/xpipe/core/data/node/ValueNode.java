package io.xpipe.core.data.node;

import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.ValueType;

import java.math.BigDecimal;
import java.math.BigInteger;
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
        var toReturn =  Arrays.equals(getRawData(), that.getRawData()) && Objects.equals(getMetaAttributes(), that.getMetaAttributes());
        if (toReturn == false) {
            throw new AssertionError();
        }
        return toReturn;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getRawData()) + Objects.hash(getMetaAttributes());
    }

    public static ValueNode nullValue() {
        return new SimpleImmutableValueNode(new byte[0]).tag(IS_NULL).asValue();
    }

    public static ValueNode of(byte[] data) {
        if (data == null) {
            return nullValue();
        }

        return new SimpleImmutableValueNode(data);
    }

    public static ValueNode ofBytes(byte[] data) {
        var created = of(data);
        created.tag(IS_BINARY);
        return created;
    }

    public static ValueNode ofInteger(BigInteger integer) {
        var created = of(integer);
        created.tag(IS_INTEGER);
        return created;
    }

    public static ValueNode ofDecimal(BigDecimal decimal) {
        var created = of(decimal);
        created.tag(IS_FLOATING_POINT);
        return created;
    }
    public static ValueNode ofBoolean(Boolean bool) {
        var created = of(bool);
        created.tag(IS_BOOLEAN);
        return created;
    }

    public static ValueNode of(Object o) {
        if (o == null) {
            return nullValue();
        }

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
