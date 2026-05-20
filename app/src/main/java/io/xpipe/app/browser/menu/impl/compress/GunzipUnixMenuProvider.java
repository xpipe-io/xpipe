package io.xpipe.app.browser.menu.impl.compress;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.icon.BrowserIconFileType;
import io.xpipe.app.browser.icon.BrowserIcons;
import io.xpipe.app.browser.menu.BrowserApplicationPathMenuProvider;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.core.OsType;

import javafx.beans.value.ObservableValue;

import java.util.List;

public class GunzipUnixMenuProvider implements BrowserMenuLeafProvider, BrowserApplicationPathMenuProvider {

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.CompGraphic(BrowserIcons.createContextMenuIcon(BrowserIconFileType.byId("zip")));
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.CUSTOM;
    }

    @Override
    public boolean automaticallyResolveLinks() {
        return false;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var dir = entries.size() > 1
                ? "[...]"
                : GunzipActionProvider.getTarget(
                                        entries.getFirst().getRawFileEntry().getPath())
                                .getFileName();
        return AppI18n.observable("gunzipDirectory", dir);
    }

    @Override
    public String getExecutable() {
        return "gunzip";
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        if (!BrowserApplicationPathMenuProvider.super.isApplicable(model, entries)
                || !BrowserMenuLeafProvider.super.isApplicable(model, entries)) {
            return false;
        }

        return entries.stream()
                        .allMatch(entry -> {
                            var s = entry.getRawFileEntry().getPath().toString();
                            if (s.endsWith(".tar.gz") || s.endsWith(".tgz") || s.equals("tar.gzip")) {
                                return false;
                            }

                            return s.endsWith(".gz") || s.endsWith(".gzip");
                                })
                && model.getFileSystem().getShell().orElseThrow().getOsType() != OsType.WINDOWS;
    }

    @Override
    public AbstractAction createAction(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var builder = GunzipActionProvider.Action.builder();
        builder.initEntries(model, entries);
        return builder.build();
    }
}
