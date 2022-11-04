package io.xpipe.extension.util;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.ValueNode;

import java.util.Currency;
import java.util.Optional;

public class DataTypeParser {

    public static Optional<ValueNode> parseMonetary(String val) {
        for (Currency availableCurrency : Currency.getAvailableCurrencies()) {
            if (val.contains(availableCurrency.getSymbol())) {
                String newStr = DataTypeParserInternal.cleanseNumberString(val);
                var node = DataTypeParserInternal.parseDecimalFromCleansed(newStr);
                if (node.isEmpty()) {
                    continue;
                }

                return Optional.of(ValueNode.ofCurrency(
                        val, node.get().getMetaAttribute(DataStructureNode.DECIMAL_VALUE), availableCurrency));
            }
        }
        return Optional.empty();
    }

    public static Optional<ValueNode> parseNumber(String val) {
        var cleansed = DataTypeParserInternal.cleanseNumberString(val);
        return DataTypeParserInternal.parseNumberFromCleansed(cleansed);
    }
}
