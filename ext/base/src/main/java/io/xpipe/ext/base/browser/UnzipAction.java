package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.ExecuteApplicationAction;
import io.xpipe.app.browser.icon.FileType;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.OsType;

import java.util.List;

public class UnzipAction extends ExecuteApplicationAction implements FileTypeAction {

    @Override
    public String getExecutable() {
        return "unzip";
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return FileTypeAction.super.isApplicable(model, entries) && !model.getFileSystem().getShell().orElseThrow().getOsType().equals(OsType.WINDOWS);
    }

    @Override
    protected String createCommand(OpenFileSystemModel model, BrowserEntry entry) {
        return "unzip -o " + entry.getOptionallyQuotedFileName() + " -d " + FileNames.quoteIfNecessary(FileNames.getBaseName(entry.getFileName()));
    }

    @Override
    protected boolean refresh() {
        return true;
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
    public FileType getType() {
        return FileType.byId("zip");
    }
}
