package io.xpipe.app.update;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.fxcomps.impl.CodeSnippet;
import io.xpipe.app.fxcomps.impl.CodeSnippetComp;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Region;

public class HomebrewUpdater extends GitHubUpdater {

    public HomebrewUpdater() {
        super(true);
    }

    @Override
    public Region createInterface() {
        var snippet = CodeSnippet.builder()
                .keyword("brew")
                .space()
                .keyword("update")
                .space()
                .identifier("&&")
                .space()
                .keyword("brew")
                .space()
                .keyword("upgrade")
                .space()
                .identifier("--cask")
                .space()
                .string("xpipe")
                .build();
        return new CodeSnippetComp(false, new SimpleObjectProperty<>(snippet)).createRegion();
    }

    @Override
    public void prepareUpdateImpl() {
        var changelogString =
                AppDownloads.downloadChangelog(lastUpdateCheckResult.getValue().getVersion(), false);
        var changelog = changelogString.orElse(null);

        var rel = new PreparedUpdate(
                AppProperties.get().getVersion(),
                XPipeDistributionType.get().getId(),
                lastUpdateCheckResult.getValue().getVersion(),
                lastUpdateCheckResult.getValue().getReleaseUrl(),
                null,
                changelog,
                lastUpdateCheckResult.getValue().getAssetType());
        preparedUpdate.setValue(rel);
    }
}
