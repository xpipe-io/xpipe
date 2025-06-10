package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.icon.BrowserIconFileType;
import io.xpipe.app.browser.menu.BrowserApplicationPathMenuProvider;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.browser.menu.FileTypeMenuProvider;
import io.xpipe.app.util.FileOpener;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;

public class JavapMenuProvider
        implements FileTypeMenuProvider, BrowserApplicationPathMenuProvider, BrowserMenuLeafProvider {

    @Override
    public boolean isMutation() {
        return true;
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.CUSTOM;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var arg = entries.size() == 1 ? entries.getFirst().getFileName() : "(" + entries.size() + ")";
        return new SimpleStringProperty("javap -c -p " + arg);
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return FileTypeMenuProvider.super.isApplicable(model, entries);
    }

    @Override
    public BrowserIconFileType getType() {
        return BrowserIconFileType.byId("class");
    }

    @Override
    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) throws Exception {
        ShellControl sc = model.getFileSystem().getShell().orElseThrow();
        for (BrowserEntry entry : entries) {
            var command = CommandBuilder.of()
                    .add("javap", "-c", "-p")
                    .addFile(entry.getRawFileEntry().getPath());
            var out = sc.command(command)
                    .withWorkingDirectory(model.getCurrentDirectory().getPath())
                    .readStdoutOrThrow();
            FileOpener.openReadOnlyString(out);
        }
    }

    @Override
    public String getExecutable() {
        return "java";
    }
}
