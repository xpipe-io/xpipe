package io.xpipe.ext.office.excel;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.source.TableDataSource;
import io.xpipe.core.source.TableReadConnection;
import io.xpipe.core.source.TableWriteConnection;
import io.xpipe.core.source.WriteMode;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.ext.office.excel.model.ExcelHeaderState;
import io.xpipe.ext.office.excel.model.ExcelRange;
import io.xpipe.ext.office.excel.model.ExcelSheetIdentifier;
import io.xpipe.extension.util.Validators;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("excel")
@SuperBuilder
@Jacksonized
@Getter
public class ExcelSource extends TableDataSource<StreamDataStore> {

    private final ExcelSheetIdentifier identifier;
    private final ExcelRange range;
    private final ExcelHeaderState headerState;
    private final boolean continueSelection;

    @Override
    public void checkComplete() throws Exception {
        super.checkComplete();
        Validators.nonNull(identifier, "Sheet");
        Validators.nonNull(headerState, "Header");
    }

    @Override
    protected TableReadConnection newReadConnection() {
        return new ExcelReadConnection(this);
    }

    @Override
    public TableWriteConnection newWriteConnection(WriteMode mode) {
        var sup = super.newWriteConnection(mode);
        if (sup != null) {
            return sup;
        }

        if (mode.equals(WriteMode.REPLACE)) {
            return new ExcelWriteConnection(this);
        }

        throw new UnsupportedOperationException(mode.getId());
    }
}
