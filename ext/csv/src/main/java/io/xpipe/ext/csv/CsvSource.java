package io.xpipe.ext.csv;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.util.Validators;
import io.xpipe.core.charsetter.Charsettable;
import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.source.TableDataSource;
import io.xpipe.core.source.TableReadConnection;
import io.xpipe.core.source.TableWriteConnection;
import io.xpipe.core.source.WriteMode;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StreamDataStore;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("csv")
@SuperBuilder
@Jacksonized
@Getter
public class CsvSource extends TableDataSource<StreamDataStore> implements Charsettable {

    StreamCharset charset;
    NewLine newLine;
    Character delimiter;
    Character quote;
    CsvHeaderState headerState;

    public static CsvSource empty(DataStore store) throws Exception {
        var result = Charsetter.get().detect(store.asNeeded());
        return CsvSource.builder()
                .store(store.asNeeded())
                .charset(result.getCharset())
                .newLine(result.getNewLine())
                .delimiter(',')
                .quote('"')
                .headerState(CsvHeaderState.INCLUDED)
                .build();
    }

    @Override
    public void checkComplete() throws Exception {
        super.checkComplete();
        Validators.nonNull(charset, "Charset");
        Validators.nonNull(newLine, "Newline");
        Validators.nonNull(delimiter, "Delimiter");
        Validators.nonNull(quote, "Quote");
        Validators.nonNull(headerState, "Header");
    }

    @Override
    public TableWriteConnection newWriteConnection(WriteMode mode) {
        var sup = super.newWriteConnection(mode);
        if (sup != null) {
            return sup;
        }

        if (mode.equals(WriteMode.REPLACE)) {
            return new CsvWriteConnection(this);
        }

        throw new UnsupportedOperationException(mode.getId());
    }

    @Override
    public TableReadConnection newReadConnection() {
        return new CsvReadConnection(this);
    }
}
