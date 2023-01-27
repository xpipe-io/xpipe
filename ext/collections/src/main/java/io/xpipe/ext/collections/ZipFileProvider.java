package io.xpipe.ext.collections;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.store.DataStore;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

public class ZipFileProvider extends ArchiveFileProvider<ZipFileProvider.Source> {

    @Override
    public Dialog configDialog(Source source, boolean all) {
        return Dialog.empty();
    }

    @Override
    public Source createDefaultSource(DataStore input) {
        return Source.builder().store(input.asNeeded()).build();
    }

    @Override
    public Class<Source> getSourceClass() {
        return Source.class;
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("zip");
    }

    @Override
    public Map<String, List<String>> getSupportedExtensions() {
        return Map.of(i18nKey("zipFileName"), List.of("zip"));
    }

    @JsonTypeName("zip")
    @SuperBuilder
    @Jacksonized
    static class Source extends ArchiveFile {

        @Override
        protected String getId() {
            return "zip";
        }
    }
}
