package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.browser.action.ExecuteApplicationAction;
import io.xpipe.app.browser.icon.BrowserIconFileType;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileNames;

import java.util.List;

public class UnzipAction extends ExecuteApplicationAction implements FileTypeAction {

    @Override
    public String getExecutable() {
        return "unzip";
    }

    @Override
    protected boolean refresh() {
        return true;
    }

    @Override
    protected String createCommand(OpenFileSystemModel model, BrowserEntry entry) {
        return "unzip -o " + entry.getOptionallyQuotedFileName() + " -d "
                + FileNames.quoteIfNecessary(FileNames.getBaseName(entry.getFileName()));
    }

    @Override
    public Category getCategory() {
        return Category.CUSTOM;
    }

    @Override
    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return "unzip [...]";
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return FileTypeAction.super.isApplicable(model, entries)
                && !model.getFileSystem().getShell().orElseThrow().getOsType().equals(OsType.WINDOWS);
    }

    @Override
    public BrowserIconFileType getType() {
        return BrowserIconFileType.byId("zip");
    }
}
