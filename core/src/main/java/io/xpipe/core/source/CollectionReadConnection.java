package io.xpipe.core.source;

import lombok.SneakyThrows;

import java.util.stream.Stream;

public interface CollectionReadConnection extends DataSourceReadConnection {

    Stream<DataSource<?>> listEntries() throws Exception;

    @SneakyThrows
    default void forward(DataSourceConnection con) {
        try (var tCon = (CollectionWriteConnection) con) {
            tCon.init();
            listEntries().forEach(s -> {
                //                try (var subCon = open(s)) {
                //                    ((CollectionWriteConnection) con).write(s, subCon);
                //                }
            });
        }
    }
}
