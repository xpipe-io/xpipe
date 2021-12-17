package io.xpipe.core.data.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.data.DataStructureNode;
import io.xpipe.core.data.type.callback.DataTypeCallback;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.List;

@JsonTypeName("tuple")
@EqualsAndHashCode
public class TupleType implements DataType {

    private List<String> names;
    private List<DataType> types;

    @JsonCreator
    private TupleType(List<String> names, List<DataType> types) {
        this.names = names;
        this.types = types;
    }

    public static TupleType empty() {
        return new TupleType(List.of(), List.of());
    }

    public static TupleType wrap(List<String> names, List<DataType> types) {
        return new TupleType(names, types);
    }

    public static TupleType wrapWithoutNames(List<DataType> types) {
        return new TupleType(Collections.nCopies(types.size(), null), types);
    }

    @Override
    public String getName() {
        return "tuple";
    }

    @Override
    public boolean matches(DataStructureNode node) {
        return node.isTuple();
    }

    @Override
    public boolean isTuple() {
        return true;
    }

    @Override
    public void traverseType(DataTypeCallback cb) {
        cb.onTupleBegin(this);
        for (var t : types) {
            t.traverseType(cb);
        }
        cb.onTupleEnd();
    }

    public int getSize() {
        return types.size();
    }

    public List<String> getNames() {
        return names;
    }

    public List<DataType> getTypes() {
        return types;
    }
}
