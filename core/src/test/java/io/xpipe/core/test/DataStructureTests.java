package io.xpipe.core.test;

import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class DataStructureTests {

    public static DataStructureNode createTestData() {
        var val = ValueNode.wrap("value".getBytes(StandardCharsets.UTF_8));
        var flatArray = ArrayNode.wrap(List.of(ValueNode.of(1), ValueNode.of(2)));
        var flatTuple = TupleNode.builder().add("key1", val).build();
        var nestedArray = ArrayNode.wrap(List.of(flatArray, flatTuple));
        return TupleNode.builder()
                .add("key1", val)
                .add("key2", flatArray)
                .add("key3", flatTuple)
                .add("key4", nestedArray)
                .build();
    }
}
