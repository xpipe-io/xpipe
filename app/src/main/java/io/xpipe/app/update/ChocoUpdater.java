package io.xpipe.app.update;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.fxcomps.impl.CodeSnippet;
import io.xpipe.app.fxcomps.impl.CodeSnippetComp;
import io.xpipe.core.store.LocalStore;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Region;

import java.time.Instant;

public class ChocoUpdater extends UpdateHandler {

    public ChocoUpdater() {
        super(true);
    }

    @Override
    public Region createInterface() {
        var snippet = CodeSnippet.builder()
                .keyword("choco")
                .space()
                .identifier("install")
                .space()
                .string("xpipe")
                .space()
                .keyword("--version=" + getPreparedUpdate().getValue().getVersion())
                .build();
        return new CodeSnippetComp(false, new SimpleObjectProperty<>(snippet)).createRegion();
    }

    public AvailableRelease refreshUpdateCheckImpl() throws Exception {
        try (var sc = new LocalStore().control().start()) {
            var latest = sc.executeSimpleStringCommand("choco outdated -r --nocolor")
                    .lines()
                    .filter(s -> s.startsWith("xpipe"))
                    .findAny()
                    .map(string -> string.split("\\|")[2]);
            if (latest.isEmpty()) {
                return null;
            }

            var isUpdate = isUpdate(latest.get());
            var rel = new AvailableRelease(
                    AppProperties.get().getVersion(),
                    XPipeDistributionType.get().getId(),
                    latest.get(),
                    "https://community.chocolatey.org/packages/xpipe/" + latest,
                    null,
                    null,
                    Instant.now(),
                    null,
                    isUpdate);
            lastUpdateCheckResult.setValue(rel);
            return lastUpdateCheckResult.getValue();
        }
    }
}
