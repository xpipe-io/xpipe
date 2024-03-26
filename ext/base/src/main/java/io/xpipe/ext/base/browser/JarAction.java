package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.BrowserActionFormatter;
import io.xpipe.app.browser.action.MultiExecuteAction;
import io.xpipe.app.browser.icon.BrowserIconFileType;
import io.xpipe.core.process.ShellControl;

import java.util.List;

public class JarAction extends MultiExecuteAction implements JavaAction, FileTypeAction {

    @Override
    public Category getCategory() {
        return Category.CUSTOM;
    }

    @Override
    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return "java -jar " + BrowserActionFormatter.filesArgument(entries);
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return super.isApplicable(model, entries) && FileTypeAction.super.isApplicable(model, entries);
    }

    @Override
    protected String createCommand(ShellControl sc, OpenFileSystemModel model, BrowserEntry entry) {
        return "java -jar " + entry.getOptionallyQuotedFileName();
    }

    @Override
    public BrowserIconFileType getType() {
        return BrowserIconFileType.byId("jar");
    }
}
