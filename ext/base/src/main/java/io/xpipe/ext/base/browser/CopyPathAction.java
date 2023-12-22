package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionFormatter;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.util.ClipboardHelper;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileNames;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

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
    public List<LeafAction> getBranchingActions(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return List.of(
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return " "
                                    + BrowserActionFormatter.centerEllipsis(
                                            entries.get(0).getRawFileEntry().getPath(), 50);
                        }

                        return "Absolute Paths";
                    }

                    @Override
                    public KeyCombination getShortcut() {
                        return new KeyCodeCombination(KeyCode.C, KeyCombination.ALT_DOWN, KeyCombination.SHORTCUT_DOWN);
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> entry.getRawFileEntry().getPath())
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                },
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return " "
                                    + BrowserActionFormatter.centerEllipsis(
                                            entries.get(0).getRawFileEntry().getPath(), 50);
                        }

                        return "Absolute Link Paths";
                    }

                    @Override
                    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return entries.stream()
                                .allMatch(browserEntry ->
                                        browserEntry.getRawFileEntry().getKind() == FileKind.LINK);
                    }

                    @Override
                    public boolean automaticallyResolveLinks() {
                        return false;
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> entry.getRawFileEntry().getPath())
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                },
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return "\""
                                    + BrowserActionFormatter.centerEllipsis(
                                            entries.get(0).getRawFileEntry().getPath(), 50)
                                    + "\"";
                        }

                        return "Absolute Paths (Quoted)";
                    }

                    @Override
                    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return entries.stream()
                                .anyMatch(entry ->
                                        entry.getRawFileEntry().getPath().contains(" "));
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> "\"" + entry.getRawFileEntry().getPath() + "\"")
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                },
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return " "
                                    + BrowserActionFormatter.centerEllipsis(
                                            FileNames.getFileName(entries.get(0)
                                                    .getRawFileEntry()
                                                    .getPath()),
                                            50);
                        }

                        return "File Names";
                    }

                    @Override
                    public KeyCombination getShortcut() {
                        return new KeyCodeCombination(
                                KeyCode.C, KeyCombination.SHIFT_DOWN, KeyCombination.SHORTCUT_DOWN);
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> FileNames.getFileName(
                                        entry.getRawFileEntry().getPath()))
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                },
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return " "
                                    + BrowserActionFormatter.centerEllipsis(
                                            FileNames.getFileName(entries.get(0)
                                                    .getRawFileEntry()
                                                    .getPath()),
                                            50);
                        }

                        return "Link File Names";
                    }

                    @Override
                    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return entries.stream()
                                        .allMatch(browserEntry ->
                                                browserEntry.getRawFileEntry().getKind() == FileKind.LINK)
                                && entries.stream().anyMatch(browserEntry -> !browserEntry
                                        .getFileName()
                                        .equals(FileNames.getFileName(browserEntry
                                                .getRawFileEntry()
                                                .resolved()
                                                .getPath())));
                    }

                    @Override
                    public boolean automaticallyResolveLinks() {
                        return false;
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> FileNames.getFileName(
                                        entry.getRawFileEntry().getPath()))
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                },
                new LeafAction() {
                    @Override
                    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return "\""
                                    + BrowserActionFormatter.centerEllipsis(
                                            FileNames.getFileName(entries.get(0)
                                                    .getRawFileEntry()
                                                    .getPath()),
                                            50)
                                    + "\"";
                        }

                        return "File Names (Quoted)";
                    }

                    @Override
                    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        return entries.stream().anyMatch(entry -> FileNames.getFileName(
                                        entry.getRawFileEntry().getPath())
                                .contains(" "));
                    }

                    @Override
                    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> "\""
                                        + FileNames.getFileName(
                                                entry.getRawFileEntry().getPath()) + "\"")
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                });
    }
}
