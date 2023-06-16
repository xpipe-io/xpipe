package io.xpipe.core.impl;

import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.source.TableDataSource;
import io.xpipe.core.source.TableMapping;
import io.xpipe.core.source.TableWriteConnection;

import java.util.Optional;

public interface SimpleTableWriteConnection<T extends TableDataSource<?>> extends TableWriteConnection {

    T getSource();

    default Optional<TupleType> getType() throws Exception {
        return getSource().determineDataType();
    }

    default Optional<TableMapping> createMapping(TupleType inputType) throws Exception {
        var outputType = getType();
        if (outputType.isEmpty() || outputType.get().getSize() == 0) {
            return Optional.of(TableMapping.createIdentity(inputType));
        }

        return TableMapping.createBasic(inputType, outputType.get());
    }
}
