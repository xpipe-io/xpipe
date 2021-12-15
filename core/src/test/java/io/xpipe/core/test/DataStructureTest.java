package io.xpipe.core.test;

import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class DataStructureTest {

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

    @Test
    public void testBasicOperations() {
        var obj = createTestData();
        Assertions.assertEquals(obj.size(), 4);
        Assertions.assertTrue(obj.isTuple());

        var objCopy = createTestData();
        Assertions.assertEquals(obj, objCopy);

        var key1 = obj.forKey("key1").asString();
        Assertions.assertEquals(key1, "value");

        var key2 = obj.forKey("key2");
        Assertions.assertTrue(key2.isArray());
        Assertions.assertEquals(key2.at(0), ValueNode.of(1));
        Assertions.assertEquals(key2.at(0).asString(), "1");
        Assertions.assertEquals(key2.at(0).asInt(), 1);

        var key3 = obj.forKey("key3");
        Assertions.assertTrue(key3.isTuple());
        Assertions.assertEquals(key3.forKey("key1"), ValueNode.of("value"));
        Assertions.assertEquals(key3.forKey("key1").asString(), "value");

        var key4 = obj.forKey("key4");
        Assertions.assertTrue(key4.isArray());
        Assertions.assertEquals(key4.at(0), ArrayNode.of(ValueNode.of(1), ValueNode.of(2)));
        Assertions.assertEquals(key4.at(0).at(0).asInt(), 1);
    }
}
