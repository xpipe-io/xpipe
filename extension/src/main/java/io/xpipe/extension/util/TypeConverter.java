package io.xpipe.extension.util;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.ValueNode;
import org.apache.commons.lang3.math.NumberUtils;

public class TypeConverter {

    public static void tagNullType(ValueNode node, String nullValue) {
        var string = node.asString();
        if (string.equals(nullValue)) {
            node.tag(DataStructureNode.NULL_VALUE);
        }
    }

    public static void tagBooleanType(ValueNode node, String trueValue, String falseValue) {
        var string = node.asString();
        if (string.equals(trueValue)) {
            node.tag(DataStructureNode.BOOLEAN_TRUE);
            node.tag(DataStructureNode.BOOLEAN_VALUE);
        }
        if (string.equals(falseValue)) {
            node.tag(DataStructureNode.BOOLEAN_FALSE);
            node.tag(DataStructureNode.BOOLEAN_VALUE);
        }
    }

    public static void tagNumberType(ValueNode node) {
        var string = node.asString();
        if (NumberUtils.isCreatable(string)) {
            node.tag(DataStructureNode.IS_NUMBER);
            var number = NumberUtils.createNumber(string);
            if (number instanceof Float || number instanceof Double) {
                node.tag(DataStructureNode.IS_FLOATING_POINT);
            } else {
                node.tag(DataStructureNode.IS_INTEGER);
            }
        }
    }
}
