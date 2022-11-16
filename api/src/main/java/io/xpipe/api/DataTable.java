package io.xpipe.api;

import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.TupleNode;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public interface DataTable extends Iterable<TupleNode>, DataSource {

    Stream<TupleNode> stream();

    ArrayNode readAll();

    ArrayNode read(int maxRows);

    default int countAndDiscard() {
        AtomicInteger count = new AtomicInteger();
        try (var stream = stream()) {
            stream.forEach(dataStructureNodes -> {
                count.getAndIncrement();
            });
        }
        return count.get();
    }
}
