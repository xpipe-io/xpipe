package io.xpipe.app.util;

import io.xpipe.core.data.node.ValueNode;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.regex.Pattern;

public class DataTypeParserInternal {

    private static final Pattern DECIMAL_IS_INTEGER = Pattern.compile("^([-\\d]+?)(\\.0*)?$");
    private static final Pattern TRAILING_ZEROS = Pattern.compile("^(.+?\\.\\d*)0+$");
    private static final Pattern LONG = Pattern.compile("^[+-]?[0-9]+$");
    private static final Pattern DECIMAL = Pattern.compile("^[+-]?([0-9]+)(\\.([0-9]+))?$");

    static String cleanseNumberString(String value) {
        value = value.replaceAll("[^-\\d.]+", "");
        return value;
    }

    static Optional<ValueNode> parseDecimalFromCleansed(String val) {
        // Normal decimal
        var simpleDecimal = parseSimpleDecimalValue(val);
        if (simpleDecimal.isPresent()) {
            return Optional.of(ValueNode.ofDecimal(val, simpleDecimal.get()));
        }

        // Specially formatted number, must be in range of double
        if (NumberUtils.isCreatable(val)) {
            var number = NumberUtils.createNumber(val);
            if (number instanceof Float || number instanceof Double) {
                return Optional.of(ValueNode.ofDecimal(val, number.doubleValue()));
            }
        }

        // Big decimal value
        try {
            var bigDecimal = new BigDecimal(val);
            return Optional.of(ValueNode.ofDecimal(bigDecimal));
        } catch (Exception e) {
        }

        return Optional.empty();
    }

    private static Optional<String> parseSimpleDecimalValue(String val) {
        var decimalMatcher = DECIMAL.matcher(val);
        if (decimalMatcher.matches()) {
            var integerMatcher = DECIMAL_IS_INTEGER.matcher(val);
            if (integerMatcher.matches()) {
                return Optional.of(integerMatcher.group(1));
            }

            var trailingRemoved = TRAILING_ZEROS.matcher(val);
            if (trailingRemoved.matches()) {
                return Optional.of(trailingRemoved.group(1));
            }

            return Optional.of(val);
        }
        return Optional.empty();
    }

    static Optional<ValueNode> parseNumberFromCleansed(String val) {
        // Simple integer
        if (LONG.matcher(val).matches()) {
            return Optional.of(ValueNode.ofInteger(val, val));
        }

        // Simple decimal
        var simpleDecimal = parseSimpleDecimalValue(val);
        if (simpleDecimal.isPresent()) {
            return Optional.of(ValueNode.ofDecimal(val, simpleDecimal.get()));
        }

        // Specially formatted number, must be in range of double or long
        if (NumberUtils.isCreatable(val)) {
            var number = NumberUtils.createNumber(val);
            if (number instanceof Float || number instanceof Double) {
                return Optional.of(ValueNode.ofDecimal(val, number.doubleValue()));
            } else {
                return Optional.of(ValueNode.ofInteger(val, number.longValue()));
            }
        }

        // Big integer value
        try {
            var bigInteger = new BigInteger(val);
            return Optional.of(ValueNode.ofInteger(bigInteger));
        } catch (Exception e) {
        }

        // Big decimal value
        try {
            var bigDecimal = new BigDecimal(val);
            return Optional.of(ValueNode.ofDecimal(bigDecimal));
        } catch (Exception e) {
        }

        return Optional.empty();
    }
}
