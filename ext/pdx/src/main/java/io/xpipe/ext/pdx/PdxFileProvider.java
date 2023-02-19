package io.xpipe.ext.pdx;

import io.xpipe.app.ext.DataSourceProviders;
import io.xpipe.app.util.UniformDataSourceProvider;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.source.*;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.ext.base.SimpleFileDataSourceProvider;
import io.xpipe.ext.pdx.savegame.SavegameType;
import lombok.experimental.SuperBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class PdxFileProvider<T extends DataSource<?>>
        implements SimpleFileDataSourceProvider<T>, UniformDataSourceProvider<T> {

    protected abstract SavegameType getType();

    protected abstract List<String> getNames();

    @Override
    public boolean supportsConversion(T desc, DataSourceType t) {
        return t == DataSourceType.COLLECTION;
    }

    @Override
    public DataSource<?> convert(T in, DataSourceType t) throws Exception {
        Source d = in.asNeeded();
        CollectionDataSource<?> ds = DataSourceProviders.byId("zip")
                .createDefaultSource(in.getStore())
                .asNeeded();
        return ds;
    }

    @Override
    public DataSourceType getPrimaryType() {
        return DataSourceType.STRUCTURE;
    }

    @Override
    public Map<String, List<String>> getSupportedExtensions() {
        var map = new LinkedHashMap<String, List<String>>();
        map.put(i18nKey("fileName"), List.of(getId()));
        return map;
    }

    @Override
    public String getDisplayIconFileName() {
        return "pdx:" + getId() + "_icon.png";
    }

    @SuperBuilder
    public abstract static class Source extends StructureDataSource<StreamDataStore> {

        @Override
        public StructureWriteConnection newWriteConnection(WriteMode mode) {
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
                        var t = getProvider().getType();
                        if (!t.matchesInput(bytes)) {
                            throw new IllegalArgumentException(
                                    "Not a valid " + getProvider().getId() + " file");
                        }

                        var struc = t.determineStructure(bytes);
                        var r = struc.parse(bytes);

                        if (r.invalid().isPresent()) {
                            throw new IllegalArgumentException(
                                    "Error while parsing file:\n" + r.invalid().get().message);
                        }

                        if (r.error().isPresent()) {
                            throw r.error().get().error;
                        }

                        return r.success().get().combinedNode();
                    }
                }
            };
        }

        protected Map<String, String> annotateContents() {
            return Map.of();
        }

        protected abstract PdxFileProvider<?> getProvider();
    }
}
