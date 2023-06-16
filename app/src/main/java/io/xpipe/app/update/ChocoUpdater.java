package io.xpipe.app.update;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.fxcomps.impl.CodeSnippet;
import io.xpipe.app.fxcomps.impl.CodeSnippetComp;
import io.xpipe.core.store.ShellStore;
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
        try (var sc = ShellStore.createLocal().control().start()) {
            var latest = sc.executeSimpleStringCommand("choco outdated -r --nocolor")
                    .lines()
                    .filter(s -> s.startsWith("xpipe"))
                    .findAny()
                    .orElseThrow()
                    .split("\\|")[2];
            var isUpdate = isUpdate(latest);
            var rel = new AvailableRelease(
                    AppProperties.get().getVersion(),
                    XPipeDistributionType.get().getId(),
                    latest,
                    "https://community.chocolatey.org/packages/xpipe/" + latest,
                    null,
                    null,
                    Instant.now(),
                    isUpdate);
            lastUpdateCheckResult.setValue(rel);
            return lastUpdateCheckResult.getValue();
        }
    }
}
