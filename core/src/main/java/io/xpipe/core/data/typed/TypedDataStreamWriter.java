package io.xpipe.core.data.typed;

import io.xpipe.core.data.generic.GenericDataStreamWriter;
import io.xpipe.core.data.node.*;
import io.xpipe.core.data.type.ArrayType;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.TupleType;

import java.io.IOException;
import java.io.OutputStream;

public class TypedDataStreamWriter {

    public static void writeStructure(OutputStream out, DataStructureNode node, DataType type) throws IOException {
        out.write(DataStructureNodeIO.TYPED_STRUCTURE_ID);
        write(out, node, type);
    }

    private static void write(OutputStream out, DataStructureNode node, DataType type) throws IOException {
        if (type.isTuple() && node.isTuple()) {
            writeTuple(out, (TupleNode) node, (TupleType) type);
        } else if (node.isArray() && type.isArray()) {
            writeArray(out, (ArrayNode) node, (ArrayType) type);
        } else if (node.isValue() && type.isValue()) {
            writeValue(out, (ValueNode) node);
        } else if (type.isWildcard()) {
            GenericDataStreamWriter.writeStructure(out, node);
        } else {
            throw new IllegalStateException("Incompatible node and type");
        }
    }

    private static void writeValue(OutputStream out, ValueNode n) throws IOException {
        out.write(DataStructureNodeIO.TYPED_VALUE_ID);
        var length = DataStructureNodeIO.writeShort(out, n.getRawData().length);
        out.write(n.getRawData(), 0, length);
        DataStructureNodeIO.writeAttributes(out, n);
    }

    private static void writeTuple(OutputStream out, TupleNode tuple, TupleType type) throws IOException {
        if (tuple.size() != type.getSize()) {
            throw new IllegalArgumentException("Tuple size mismatch");
        }

        out.write(DataStructureNodeIO.TYPED_TUPLE_ID);
        for (int i = 0; i < tuple.size(); i++) {
            write(out, tuple.at(i), type.getTypes().get(i));
        }
        DataStructureNodeIO.writeAttributes(out, tuple);
    }

    private static void writeArray(OutputStream out, ArrayNode array, ArrayType type) throws IOException {
        out.write(DataStructureNodeIO.TYPED_ARRAY_ID);
        DataStructureNodeIO.writeShort(out, array.size());
        for (int i = 0; i < array.size(); i++) {
            write(out, array.at(i), type.getSharedType());
        }
        DataStructureNodeIO.writeAttributes(out, array);
    }
}
