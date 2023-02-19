package io.xpipe.ext.pdx;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.ext.DataSourceProviders;
import io.xpipe.core.store.DataFlow;
import io.xpipe.core.store.DataStore;
import io.xpipe.ext.pdx.savegame.SavegameType;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

public class Eu4FileProvider extends PdxFileProvider<Eu4FileProvider.Source> {

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
        return List.of("eu4");
    }

    @Override
    protected SavegameType getType() {
        return SavegameType.EU4;
    }

    @Override
    protected List<String> getNames() {
        return List.of(getId());
    }

    @JsonTypeName("eu4")
    @SuperBuilder
    @Jacksonized
    public static class Source extends PdxFileProvider.Source {

        @Override
        public DataFlow getFlow() {
            return DataFlow.INPUT;
        }

        @Override
        protected PdxFileProvider<?> getProvider() {
            return DataSourceProviders.byId("eu4");
        }

        @Override
        protected Map<String, String> annotateContents() {
            return Map.of("gamestate", "pdxText");
        }
    }
}
