package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.icon.BrowserIconFileType;
import io.xpipe.app.browser.menu.BrowserApplicationPathMenuProvider;
import io.xpipe.app.browser.menu.FileTypeMenuProvider;
import io.xpipe.app.browser.menu.MultiExecuteMenuProvider;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public class JarMenuProvider extends MultiExecuteMenuProvider
        implements BrowserApplicationPathMenuProvider, FileTypeMenuProvider {

    @Override
    public boolean isMutation() {
        return true;
    }

    @Override
    public Category getCategory() {
        return Category.CUSTOM;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var arg = entries.size() == 1 ? entries.getFirst().getFileName() : "(" + entries.size() + ")";
        return new SimpleStringProperty("java -jar " + arg);
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return super.isApplicable(model, entries) && FileTypeMenuProvider.super.isApplicable(model, entries);
    }

    @Override
    protected CommandBuilder createCommand(ShellControl sc, BrowserFileSystemTabModel model, BrowserEntry entry) {
        return CommandBuilder.of()
                .add("java", "-jar")
                .addFile(entry.getRawFileEntry().getPath());
    }

    @Override
    public BrowserIconFileType getType() {
        return BrowserIconFileType.byId("jar");
    }

    @Override
    public String getExecutable() {
        return "java";
    }
}
