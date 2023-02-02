package io.xpipe.ext.office.docx;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.source.*;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.ext.base.SimpleFileDataSourceProvider;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

public class DocxProvider implements SimpleFileDataSourceProvider<DocxProvider.Source> {

    @Override
    public Dialog configDialog(Source source, boolean all) {
        return null;
    }

    @Override
    public DataSourceType getPrimaryType() {
        return DataSourceType.TEXT;
    }

    @Override
    public Map<String, List<String>> getSupportedExtensions() {
        return Map.of(i18nKey("fileName"), List.of("docx"));
    }

    @Override
    public Source createDefaultSource(DataStore input) throws Exception {
        return Source.builder().store(input.asNeeded()).build();
    }

    @Override
    public Class<Source> getSourceClass() {
        return Source.class;
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("docx");
    }

    @JsonTypeName("docx")
    @SuperBuilder
    @Jacksonized
    public static class Source extends TextDataSource<StreamDataStore> {

        @Override
        protected TextWriteConnection newWriteConnection(WriteMode mode) {
            var sup = super.newWriteConnection(mode);
            if (sup != null) {
                return sup;
            }

            if (mode.equals(WriteMode.REPLACE)) {
                return new DocxWriteConnection();
            }

            throw new UnsupportedOperationException(mode.getId());
        }

        @Override
        protected TextReadConnection newReadConnection() {
            return new DocxReadConnection(getStore());
        }
    }
}
