package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionFormatter;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.core.impl.FileNames;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.stream.Collectors;

public class CopyPathAction implements BrowserAction, BranchAction {

    @Override
    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return "Copy location";
    }

    @Override
    public Category getCategory() {
        return Category.COPY_PASTE;
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public List<LeafAction> getBranchingActions() {
        return List.of(
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return " " + BrowserActionFormatter.centerEllipsis(entries.get(0).getRawFileEntry().getPath(), 50);
                        }

                        return "Absolute Path";
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
                        var s = entries.stream()
                                .map(entry -> entry.getRawFileEntry().getPath())
                                .collect(Collectors.joining("\n"));
                        var selection = new StringSelection(s);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(selection, selection);
                    }
                },
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return "\"" + BrowserActionFormatter.centerEllipsis(entries.get(0).getRawFileEntry().getPath(), 50) + "\"";
                        }

                        return "Absolute Path (Quoted)";
                    }

                    @Override
                    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return entries.stream().anyMatch(entry -> entry.getRawFileEntry().getPath().contains(" "));
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
                        var s = entries.stream()
                                .map(entry -> "\"" + entry.getRawFileEntry().getPath() + "\"")
                                .collect(Collectors.joining("\n"));
                        var selection = new StringSelection(s);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(selection, selection);
                    }
                },
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return " " + BrowserActionFormatter.centerEllipsis(FileNames.getFileName(entries.get(0).getRawFileEntry().getPath()), 50);
                        }

                        return "File Name";
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
                        var s = entries.stream()
                                .map(entry ->
                                        FileNames.getFileName(entry.getRawFileEntry().getPath()))
                                .collect(Collectors.joining("\n"));
                        var selection = new StringSelection(s);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(selection, selection);
                    }
                },
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return "\"" + BrowserActionFormatter.centerEllipsis(FileNames.getFileName(entries.get(0).getRawFileEntry().getPath()), 50) + "\"";
                        }

                        return "File Name (Quoted)";
                    }

                    @Override
                    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return entries.stream().anyMatch(entry -> FileNames.getFileName(entry.getRawFileEntry().getPath()).contains(" "));
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
                        var s = entries.stream()
                                .map(entry ->
                                             "\"" + FileNames.getFileName(entry.getRawFileEntry().getPath()) + "\"")
                                .collect(Collectors.joining("\n"));
                        var selection = new StringSelection(s);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(selection, selection);
                    }
                });
    }
}
