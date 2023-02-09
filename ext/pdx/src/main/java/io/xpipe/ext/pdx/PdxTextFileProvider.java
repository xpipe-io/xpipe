package io.xpipe.ext.pdx;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.source.*;
import io.xpipe.core.store.DataFlow;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.ext.base.SimpleFileDataSourceProvider;
import io.xpipe.ext.pdx.parser.TextFormatParser;
import io.xpipe.extension.util.UniformDataSourceProvider;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PdxTextFileProvider
        implements UniformDataSourceProvider<PdxTextFileProvider.Source>,
                SimpleFileDataSourceProvider<PdxTextFileProvider.Source> {

    @Override
    public Source createDefaultSource(DataStore input) throws Exception {
        return Source.builder().store(input.asNeeded()).build();
    }

    @Override
    public DataSourceType getPrimaryType() {
        return DataSourceType.STRUCTURE;
    }

    @Override
    public Class<Source> getSourceClass() {
        return Source.class;
    }

    @Override
    public String getNameI18nKey() {
        return i18nKey("fileName");
    }

    @Override
    public Map<String, List<String>> getSupportedExtensions() {
        var map = new LinkedHashMap<String, List<String>>();
        map.put(i18nKey("fileName"), null);
        return map;
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("pdxText");
    }

    @Override
    public String getDisplayIconFileName() {
        return "pdx:pdxText_icon.png";
    }

    @JsonTypeName("pdxText")
    @SuperBuilder
    @Jacksonized
    public static class Source extends StructureDataSource<StreamDataStore> {

        @Override
        public DataFlow getFlow() {
            return DataFlow.INPUT;
        }

        @Override
        protected StructureWriteConnection newWriteConnection(WriteMode mode) {
            return null;
        }

        @Override
        public StructureReadConnection newReadConnection() {
            var s = store.toString();
            return new StructureReadConnection() {
                @Override
                public boolean canRead() throws Exception {
                    return store.canOpen();
                }

                @Override
                public DataStructureNode read() throws Exception {
                    try (var in = store.openInput()) {
                        var bytes = in.readAllBytes();
                        var node = TextFormatParser.text().parse(store.toString(), bytes, 0, false);
                        return node;
                    }
                }
            };
        }
    }
}
