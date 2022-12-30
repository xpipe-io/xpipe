package io.xpipe.api.test;

import io.xpipe.api.DataTableAccumulator;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.type.ValueType;
import org.junit.jupiter.api.Test;

import java.util.List;

public class DataTableAccumulatorTest extends ApiTest {

    @Test
    public void test() {
        var type = TupleType.of(List.of("col1", "col2"), List.of(ValueType.of(), ValueType.of()));
        var acc = DataTableAccumulator.create(type);

        var val = type.convert(TupleNode.of(List.of(ValueNode.of("val1"), ValueNode.of("val2"))))
                .orElseThrow();
        acc.add(val);
        var table = acc.finish(":test");

        // Assertions.assertEquals(table.getInfo().getDataType(), TupleType.tableType(List.of("col1", "col2")));
        // Assertions.assertEquals(table.getInfo().getRowCountIfPresent(), OptionalInt.empty());
        // var read = table.read(1).at(0);
        // Assertions.assertEquals(val, read);
    }
}
