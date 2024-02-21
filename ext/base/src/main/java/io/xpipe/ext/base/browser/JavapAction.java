package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.BrowserActionFormatter;
import io.xpipe.app.browser.action.ToFileCommandAction;
import io.xpipe.app.browser.icon.FileType;

import java.util.List;

public class JavapAction extends ToFileCommandAction implements FileTypeAction, JavaAction {

    @Override
    public Category getCategory() {
        return Category.CUSTOM;
    }

    @Override
    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return "javap -c -p " + BrowserActionFormatter.filesArgument(entries);
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return super.isApplicable(model, entries) && FileTypeAction.super.isApplicable(model, entries);
    }

    @Override
    public FileType getType() {
        return FileType.byId("class");
    }

    @Override
    protected String createCommand(OpenFileSystemModel model, BrowserEntry entry) {
        return "javap -c -p " + entry.getOptionallyQuotedFileName();
    }
}
