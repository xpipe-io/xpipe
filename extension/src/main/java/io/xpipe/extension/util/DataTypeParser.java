package io.xpipe.extension.util;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.ValueNode;

import java.util.Currency;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DataTypeParser {

    private static final Map<String, Currency> currencies =
            Currency.getAvailableCurrencies().stream().collect(Collectors.toMap(currency -> currency.getSymbol(), currency -> currency));

    public static Optional<ValueNode> parseMonetary(String val) {
        for (var availableCurrency : currencies.entrySet()) {
            if (val.contains(availableCurrency.getKey())) {
                String newStr = DataTypeParserInternal.cleanseNumberString(val);
                var node = DataTypeParserInternal.parseDecimalFromCleansed(newStr);
                if (node.isEmpty()) {
                    continue;
                }

                return Optional.of(ValueNode.ofCurrency(
                        val, node.get().getMetaAttribute(DataStructureNode.DECIMAL_VALUE), availableCurrency.getValue()));
            }
        }
        return Optional.empty();
    }

    public static Optional<ValueNode> parseNumber(String val) {
        var cleansed = DataTypeParserInternal.cleanseNumberString(val);
        return DataTypeParserInternal.parseNumberFromCleansed(cleansed);
    }
}
