package io.xpipe.core.test;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;
import io.xpipe.core.data.type.*;
import lombok.AllArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class DataStructureTests {

    @AllArgsConstructor

    public static enum TypedDataset {

        // Variety
        DATA_1(createTestDataType1(), List.of(createTestData11(), createTestData12())),

        // Multiple nested arrays
        DATA_2(createTestDataType2(), List.of(createTestData21(), createTestData22())),

        // Array with wildcard type
        DATA_3(createTestData31().determineDataType(), List.of(createTestData31(), createTestData32())),

        // Simple values
        DATA_4(ValueType.of(), List.of(createTestData41(), createTestData42())),

        // Array with wildcard type
        DATA_5(createTestDataType5(), List.of(createTestData51(), createTestData52(), createTestData53())),

        // Tuple with wildcard type
        DATA_6(createTestDataType6(), List.of(createTestData61(), createTestData62())),

        // Wildcard type
        DATA_7(createTestDataType7(), List.of(createTestData71(), createTestData72(), createTestData73()));


        public DataType type;
        public List<DataStructureNode> nodes;

    }

    private static DataStructureNode createTestData11() {
        var val = ValueNode.of("value".getBytes(StandardCharsets.UTF_8));
        var flatArray = ArrayNode.of(List.of(ValueNode.of(1), ValueNode.of(2)));
        var flatTuple = TupleNode.builder().add("key1", val).build();
        var nestedArray = ArrayNode.of(List.of(flatArray, flatTuple));
        return TupleNode.builder()
                .add("key1", val)
                .add("key2", flatArray)
                .add("key3", flatTuple)
                .add("key4", nestedArray)
                .build();
    }

    private static DataStructureNode createTestData12() {
        var val = ValueNode.nullValue();
        var flatArray = ArrayNode.of();
        var flatTuple = TupleNode.builder().add("key1", val).build();
        var nestedArray = ArrayNode.of(List.of(flatArray, flatTuple));
        return TupleNode.builder()
                .add("key1", val)
                .add("key2", flatArray)
                .add("key3", flatTuple)
                .add("key4", nestedArray)
                .build();
    }

    private static DataType createTestDataType1() {
        return createTestData11().determineDataType();
    }

    public static DataStructureNode createTestData21() {
        var val = ValueNode.of("value".getBytes(StandardCharsets.UTF_8));
        var flatArray = ArrayNode.of(List.of(ValueNode.of(1), ValueNode.of(2)));
        var flatTuple = TupleNode.builder().add("key1", val).build();
        var nestedArray = ArrayNode.of(List.of(flatArray, flatTuple));
        var doubleNested = ArrayNode.of(val, flatArray, flatTuple, nestedArray);
        return doubleNested;
    }

    public static DataStructureNode createTestData22() {
        var val = ValueNode.of("value".getBytes(StandardCharsets.UTF_8));
        return ArrayNode.of(val);
    }

    public static DataType createTestDataType2() {
        return ArrayType.of(WildcardType.of());
    }

    public static DataStructureNode createTestData31() {
        var val = ValueNode.of("value".getBytes(StandardCharsets.UTF_8));
        var flatTuple = TupleNode.builder().add("key1", val).build();
        var flatArray = ArrayNode.of(List.of(val, flatTuple));
        return flatArray;
    }

    public static DataStructureNode createTestData32() {
        var val = ValueNode.of("value2".getBytes(StandardCharsets.UTF_8));
        var flatTuple = TupleNode.builder().add("key1", ValueNode.nullValue()).add("key2", ValueNode.nullValue()).build();
        var flatArray = ArrayNode.of(List.of(val, flatTuple));
        return flatArray;
    }

    public static DataStructureNode createTestData41() {
        var val = ValueNode.of("value".getBytes(StandardCharsets.UTF_8));
        return val;
    }

    public static DataStructureNode createTestData42() {
        var val = ValueNode.nullValue();
        return val;
    }

    public static DataStructureNode createTestData51() {
        var val = ValueNode.of("value".getBytes(StandardCharsets.UTF_8));
        var flatArray = ArrayNode.of(List.of(val, ValueNode.nullValue()));
        var array1 = ArrayNode.of(List.of(flatArray));
        var array2 = ArrayNode.of(List.of(array1, array1));
        return array2;
    }

    public static DataStructureNode createTestData52() {
        var val = ValueNode.of("value2".getBytes(StandardCharsets.UTF_8));
        var flatArray = ArrayNode.of(List.of(val));
        return flatArray;
    }

    public static DataStructureNode createTestData53() {
        var val = ValueNode.of("value2".getBytes(StandardCharsets.UTF_8));
        var flatTuple = TupleNode.builder().add("key1", val).build();
        var flatArray = ArrayNode.of(List.of(flatTuple, val));
        return flatArray;
    }

    public static DataType createTestDataType5() {
        return ArrayType.of(WildcardType.of());
    }

    public static DataStructureNode createTestData61() {
        var val = ValueNode.of("value".getBytes(StandardCharsets.UTF_8));
        var array = ArrayNode.of(List.of(val, ValueNode.nullValue()));
        var tuple = TupleNode.builder()
                .add("key1", val).add("key2", array).build();
        return tuple;
    }

    public static DataStructureNode createTestData62() {
        var val = ValueNode.of("value2".getBytes(StandardCharsets.UTF_8));
        var flatTuple = TupleNode.builder().add("key1", val).build();

        var tuple = TupleNode.builder()
                .add("key1", flatTuple).add("key2", val).build();
        return tuple;
    }

    public static DataType createTestDataType6() {
        return TupleType.of(List.of("key1", "key2"), List.of(WildcardType.of(), WildcardType.of()));
    }

    public static DataStructureNode createTestData71() {
        return ValueNode.of("value".getBytes(StandardCharsets.UTF_8));
    }

    public static DataStructureNode createTestData72() {
        var val = ValueNode.of("value2".getBytes(StandardCharsets.UTF_8));
        return TupleNode.builder().add("key1", val).build();
    }

    public static DataStructureNode createTestData73() {
        var val = ValueNode.of("value".getBytes(StandardCharsets.UTF_8));
        var array = ArrayNode.of(List.of(val, ValueNode.nullValue()));
        return array;
    }

    public static DataType createTestDataType7() {
        return WildcardType.of();
    }
}
