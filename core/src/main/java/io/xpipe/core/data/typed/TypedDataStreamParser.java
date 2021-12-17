package io.xpipe.core.data.typed;

import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.DataStructureNodeIO;
import io.xpipe.core.data.generic.GenericDataStreamParser;
import io.xpipe.core.data.generic.GenericDataStructureNodeReader;
import io.xpipe.core.data.type.ArrayType;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.data.type.TupleType;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public class TypedDataStreamParser {

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

    public void readStructures(InputStream in, TypedAbstractReader cb, Consumer<DataStructureNode> consumer) throws IOException {
        while (hasNext(in)) {
            cb.onNodeBegin();
            read(in, cb, dataType);
            cb.onNodeEnd();
            consumer.accept(cb.create());
        }
    }

    public DataStructureNode readStructure(InputStream in, TypedAbstractReader cb) throws IOException {
        if (!hasNext(in)) {
            throw new IllegalStateException("No structure to read");
        }

        cb.onNodeBegin();
        read(in, cb, dataType);
        cb.onNodeEnd();
        return cb.create();
    }

    private void read(InputStream in, TypedDataStreamCallback cb, DataType type) throws IOException {
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
                readTypedTuple(in, cb, tt);
            }
            case DataStructureNodeIO.TYPED_ARRAY_ID -> {
                if (!type.isArray()) {
                    throw new IllegalStateException("Got array but expected " + type.getName());
                }

                var at = (ArrayType) type;
                readTypedArray(in, cb, at);
            }
            case DataStructureNodeIO.TYPED_VALUE_ID -> {
                if (!type.isValue()) {
                    throw new IllegalStateException("Got value but expected " + type.getName());
                }

                readValue(in, cb);
            }
            default -> throw new IllegalStateException("Unexpected value: " + b);
        }
    }

    private void readTypedTuple(InputStream in, TypedDataStreamCallback cb, TupleType type) throws IOException {
        cb.onTupleBegin(type);
        for (int i = 0; i < type.getSize(); i++) {
            if (type.getTypes().get(i).isWildcard()) {
                var r = getGenericReader();
                GenericDataStreamParser.read(in, r);
                var node = r.create();
                cb.onGenericNode(node);
            } else {
                read(in, cb, type.getTypes().get(i));
            }
        }
        cb.onTupleEnd();
    }

    private DataType dataType;
    private GenericDataStructureNodeReader genericReader;

    public TypedDataStreamParser(DataType dataType) {
        this.dataType = dataType;
    }

    private GenericDataStructureNodeReader getGenericReader() {
        if (genericReader == null) {
            genericReader = new GenericDataStructureNodeReader();
        }
        return genericReader;
    }

    private void readTypedArray(InputStream in, TypedDataStreamCallback cb, ArrayType type) throws IOException {
        var size = in.read();
        cb.onArrayBegin(size);
        for (int i = 0; i < size; i++) {
            if (type.getSharedType().isWildcard()) {
                var r = getGenericReader();
                GenericDataStreamParser.read(in, r);
                var node = r.create();
                cb.onGenericNode(node);
            } else {
                read(in, cb, type.getSharedType());
            }
        }
        cb.onArrayEnd();
    }

    private void readValue(InputStream in, TypedDataStreamCallback cb) throws IOException {
        var size = in.read();
        var data = in.readNBytes(size);
        cb.onValue(data);
    }
}
