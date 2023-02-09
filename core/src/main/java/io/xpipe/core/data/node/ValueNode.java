package io.xpipe.core.data.node;

import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.ValueType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Currency;
import java.util.Objects;

public abstract class ValueNode extends DataStructureNode {

    protected ValueNode() {}

    public static ValueNode nullValue() {
        return new SimpleValueNode(new byte[0]).tag(IS_NULL).asValue();
    }

    public static ValueNode of(byte[] data) {
        if (data == null) {
            return nullValue();
        }

        return new SimpleValueNode(data);
    }

    public static ValueNode ofDate(String raw, Instant instant) {
        var created = of(raw);
        created.tag(IS_DATE);
        created.tag(DATE_VALUE, instant.toString());
        return created;
    }

    public static ValueNode ofDecimal(String raw, double decimal) {
        return ofDecimal(raw, String.valueOf(decimal));
    }

    public static ValueNode ofDecimal(String raw, String decimal) {
        var created = of(raw);
        created.tag(IS_DECIMAL);
        created.tag(DECIMAL_VALUE, decimal);
        return created;
    }

    public static ValueNode ofInteger(String raw, long integer) {
        return ofInteger(raw, String.valueOf(integer));
    }

    public static ValueNode ofInteger(String raw, String integer) {
        var created = of(raw);
        created.tag(IS_INTEGER);
        created.tag(INTEGER_VALUE, integer);
        return created;
    }

    public static ValueNode ofCurrency(String raw, String decimal, Currency currency) {
        var created = ofDecimal(raw, decimal);
        created.tag(IS_CURRENCY);
        created.tag(CURRENCY_CODE, currency.getCurrencyCode());
        return created;
    }

    public static ValueNode ofBytes(byte[] data) {
        var created = of(data);
        if (data != null) {
            created.tag(IS_BINARY);
        }
        return created;
    }

    public static ValueNode ofText(String text) {
        var created = of(text);
        if (text != null) {
            created.tag(IS_TEXT);
        }
        return created;
    }

    public static ValueNode ofInteger(int integer) {
        var created = of(integer);
        created.tag(IS_INTEGER);
        created.tag(INTEGER_VALUE, integer);
        return created;
    }

    public static ValueNode ofInteger(BigInteger integer) {
        var created = of(integer);
        if (integer != null) {
            created.tag(IS_INTEGER);
            created.tag(INTEGER_VALUE, integer);
        }
        return created;
    }

    public static ValueNode ofDecimal(double decimal) {
        var created = of(decimal);
        created.tag(IS_DECIMAL);
        created.tag(DECIMAL_VALUE, decimal);
        return created;
    }

    public static ValueNode ofDecimal(BigDecimal decimal) {
        var created = of(decimal);
        if (decimal != null) {
            created.tag(IS_DECIMAL);
            created.tag(DECIMAL_VALUE, decimal);
        }
        return created;
    }

    public static ValueNode ofBoolean(Boolean bool) {
        var created = of(bool);
        if (bool != null) {
            created.tag(IS_BOOLEAN);
            created.tag(bool ? BOOLEAN_TRUE : BOOLEAN_FALSE);
        }
        return created;
    }

    public static ValueNode of(Object o) {
        if (o == null) {
            return nullValue();
        }

        return of(o.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ValueNode that)) {
            return false;
        }
        var toReturn = Arrays.equals(getRawData(), that.getRawData())
                && Objects.equals(getMetaAttributes(), that.getMetaAttributes());

        // Useful for debugging
        if (toReturn == false) {
            return false;
        }

        return toReturn;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getRawData()) + Objects.hash(getMetaAttributes());
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
