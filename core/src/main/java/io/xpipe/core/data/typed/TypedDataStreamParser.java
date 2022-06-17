package io.xpipe.core.data.typed;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.DataStructureNodeIO;
import io.xpipe.core.data.generic.GenericDataStreamParser;
import io.xpipe.core.data.generic.GenericDataStructureNodeReader;
import io.xpipe.core.data.type.ArrayType;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.TupleType;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public class TypedDataStreamParser {

    private final DataType dataType;
    private GenericDataStructureNodeReader genericReader;

    public TypedDataStreamParser(DataType dataType) {
        this.dataType = dataType;
    }

    public boolean hasNext(InputStream in) throws IOException {
        var b = in.read();
        if (b == -1) {
            return false;
        }

        if (b != DataStructureNodeIO.TYPED_STRUCTURE_ID) {
            throw new IllegalStateException("Unexpected value: " + b);
        }

        return true;
    }

    public void parseStructures(InputStream in, TypedAbstractReader cb, Consumer<DataStructureNode> consumer) throws IOException {
        while (hasNext(in)) {
            cb.onNodeBegin();
            parse(in, cb, dataType);
            cb.onNodeEnd();
            consumer.accept(cb.create());
        }
    }

    public DataStructureNode parseStructure(InputStream in, TypedAbstractReader cb) throws IOException {
        if (!hasNext(in)) {
            throw new IllegalStateException("No structure to read");
        }

        cb.onNodeBegin();
        parse(in, cb, dataType);
        cb.onNodeEnd();
        return cb.create();
    }

    public void parse(InputStream in, TypedDataStreamCallback cb) throws IOException {
        parse(in, cb, dataType);
    }

    private void parse(InputStream in, TypedDataStreamCallback cb, DataType type) throws IOException {
        var b = in.read();

        // Skip
        if (b == DataStructureNodeIO.TYPED_STRUCTURE_ID) {
            b = in.read();
        }

        switch (b) {
            case DataStructureNodeIO.TYPED_TUPLE_ID -> {
                if (!type.isTuple()) {
                    throw new IllegalStateException("Got tuple but expected " + type.getName());
                }

                var tt = (TupleType) type;
                parseTypedTuple(in, cb, tt);
            }
            case DataStructureNodeIO.TYPED_ARRAY_ID -> {
                if (!type.isArray()) {
                    throw new IllegalStateException("Got array but expected " + type.getName());
                }

                var at = (ArrayType) type;
                parseTypedArray(in, cb, at);
            }
            case DataStructureNodeIO.TYPED_VALUE_ID -> {
                if (!type.isValue()) {
                    throw new IllegalStateException("Got value but expected " + type.getName());
                }

                parseValue(in, cb);
            }
            case DataStructureNodeIO.GENERIC_STRUCTURE_ID -> {
                if (!type.isWildcard()) {
                    throw new IllegalStateException("Got structure but expected " + type.getName());
                }

                GenericDataStreamParser.parse(in, getGenericReader());
                cb.onGenericNode(getGenericReader().create());
            }
            default -> {
                throw new IllegalStateException("Unexpected type id: " + b);
            }
        }
    }

    private void parseTypedTuple(InputStream in, TypedDataStreamCallback cb, TupleType type) throws IOException {
        cb.onTupleBegin(type);
        for (int i = 0; i < type.getSize(); i++) {
            parse(in, cb, type.getTypes().get(i));
        }
        cb.onTupleEnd();
    }

    private GenericDataStructureNodeReader getGenericReader() {
        if (genericReader == null) {
            genericReader = new GenericDataStructureNodeReader();
        }
        return genericReader;
    }

    private void parseTypedArray(InputStream in, TypedDataStreamCallback cb, ArrayType type) throws IOException {
        var size = in.read();
        cb.onArrayBegin(size);
        for (int i = 0; i < size; i++) {
            parse(in, cb, type.getSharedType());
        }
        cb.onArrayEnd();
    }

    private void parseValue(InputStream in, TypedDataStreamCallback cb) throws IOException {
        var type = in.read();
        if (type == DataStructureNodeIO.VALUE_TYPE_NULL) {
            cb.onValue(null, false);
            return;
        }

        var textual = type == DataStructureNodeIO.VALUE_TYPE_TEXT;
        var size = in.read();
        var data = in.readNBytes(size);
        cb.onValue(data, textual);
    }
}
