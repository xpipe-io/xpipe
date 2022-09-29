package io.xpipe.extension.util;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.ValueNode;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.math.BigInteger;

public class TypeConverter {

    public static void tagNullType(ValueNode node, String nullValue) {
        var string = node.asString();
        if (string.equals(nullValue)) {
            node.tag(DataStructureNode.IS_NULL);
        }
    }

    public static void tagBooleanType(ValueNode node, String trueValue, String falseValue) {
        var string = node.asString();
        if (string.equals(trueValue)) {
            node.tag(DataStructureNode.BOOLEAN_TRUE);
            node.tag(DataStructureNode.IS_BOOLEAN);
        }
        if (string.equals(falseValue)) {
            node.tag(DataStructureNode.BOOLEAN_FALSE);
            node.tag(DataStructureNode.IS_BOOLEAN);
        }
    }

    public static void tagNumberType(ValueNode node) {
        var string = node.asString();
        if (NumberUtils.isCreatable(string)) {
            var number = NumberUtils.createNumber(string);
            if (number instanceof Float || number instanceof Double) {
                node.tag(DataStructureNode.IS_FLOATING_POINT);
            } else {
                node.tag(DataStructureNode.IS_INTEGER);
            }
        }
    }

    public static BigDecimal parseDecimal(DataStructureNode node) {
        if (node == null || node.hasMetaAttribute(DataStructureNode.IS_NULL)) {
            return BigDecimal.ZERO;
        }

        if (node.hasMetaAttribute(DataStructureNode.FLOATING_POINT_VALUE)) {
            return new BigDecimal(node.getMetaAttribute(DataStructureNode.FLOATING_POINT_VALUE));
        }

        var parsedDecimal = parseDecimal(node.asString());
        if (parsedDecimal != null) {
            return parsedDecimal;
        }

        if (node.hasMetaAttribute(DataStructureNode.INTEGER_VALUE)) {
            return new BigDecimal(node.getMetaAttribute(DataStructureNode.INTEGER_VALUE));
        }

        var parsedInteger = parseInteger(node.asString());
        if (parsedInteger != null) {
            return new BigDecimal(parsedInteger);
        }

        return null;
    }

    public static Boolean parseBoolean(DataStructureNode node) {
        if (node == null || node.hasMetaAttribute(DataStructureNode.IS_NULL)) {
            return false;
        }

        if (node.hasMetaAttribute(DataStructureNode.BOOLEAN_FALSE)) {
            return Boolean.parseBoolean(node.getMetaAttribute(DataStructureNode.BOOLEAN_FALSE));
        }

        if (node.hasMetaAttribute(DataStructureNode.BOOLEAN_TRUE)) {
            return Boolean.parseBoolean(node.getMetaAttribute(DataStructureNode.BOOLEAN_TRUE));
        }

        var string = node.asString();
        if (string.length() == 0 || string.equalsIgnoreCase("false")) {
            return false;
        }

        return true;
    }

    public static BigInteger parseInteger(DataStructureNode node) {
        if (node == null || node.hasMetaAttribute(DataStructureNode.IS_NULL)) {
            return BigInteger.ZERO;
        }

        if (node.hasMetaAttribute(DataStructureNode.INTEGER_VALUE)) {
            return new BigInteger(node.getMetaAttribute(DataStructureNode.INTEGER_VALUE));
        }

        var parsedInteger = parseInteger(node.asString());
        if (parsedInteger != null) {
            return parsedInteger;
        }

        if (node.hasMetaAttribute(DataStructureNode.FLOATING_POINT_VALUE)) {
            return new BigDecimal(node.getMetaAttribute(DataStructureNode.FLOATING_POINT_VALUE)).toBigInteger();
        }

        var parsedDecimal = parseDecimal(node.asString());
        if (parsedDecimal != null) {
            return parsedDecimal.toBigInteger();
        }

        return null;
    }

    private static BigInteger parseInteger(String string) {
        if (string == null) {
            return BigInteger.ZERO;
        }

        return NumberUtils.createBigInteger(string);
    }

    private static BigDecimal parseDecimal(String string) {
        if (string == null) {
            return BigDecimal.ZERO;
        }

        return NumberUtils.createBigDecimal(string);
    }
}
