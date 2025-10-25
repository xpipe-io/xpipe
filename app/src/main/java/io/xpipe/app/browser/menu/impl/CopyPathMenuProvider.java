package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.BrowserMenuBranchProvider;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.FileKind;
import io.xpipe.app.platform.ClipboardHelper;
import io.xpipe.app.platform.LabelGraphic;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.List;
import java.util.stream.Collectors;

public class CopyPathMenuProvider implements BrowserMenuBranchProvider {

    private static String centerEllipsis(String input, int length) {
        if (input == null) {
            return "";
        }

        if (input.length() <= length) {
            return input;
        }

        var half = (length / 2) - 5;
        return input.substring(0, half) + " ... " + input.substring(input.length() - half);
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2c-content-copy");
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.COPY_PASTE;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("copyLocation");
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public List<BrowserMenuLeafProvider> getBranchingActions(
            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return List.of(
                new BrowserMenuLeafProvider() {
                    @Override
                    public KeyCombination getShortcut() {
                        return new KeyCodeCombination(KeyCode.C, KeyCombination.ALT_DOWN, KeyCombination.SHORTCUT_DOWN);
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>(centerEllipsis(
                                    entries.getFirst()
                                            .getRawFileEntry()
                                            .getPath()
                                            .toString(),
                                    50));
                        }

                        return AppI18n.observable("absolutePaths");
                    }

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> entry.getRawFileEntry().getPath().toString())
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                },
                new BrowserMenuLeafProvider() {
                    @Override
                    public boolean automaticallyResolveLinks() {
                        return false;
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>(centerEllipsis(
                                    entries.getFirst()
                                            .getRawFileEntry()
                                            .getPath()
                                            .toString(),
                                    50));
                        }

                        return AppI18n.observable("absoluteLinkPaths");
                    }

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> entry.getRawFileEntry().getPath().toString())
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }

                    @Override
                    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return entries.stream()
                                .allMatch(browserEntry ->
                                        browserEntry.getRawFileEntry().getKind() == FileKind.LINK);
                    }
                },
                new BrowserMenuLeafProvider() {
                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>("\""
                                    + centerEllipsis(
                                            entries.getFirst()
                                                    .getRawFileEntry()
                                                    .getPath()
                                                    .toString(),
                                            50) + "\"");
                        }

                        return AppI18n.observable("absolutePathsQuoted");
                    }

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> "\"" + entry.getRawFileEntry().getPath() + "\"")
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }

                    @Override
                    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return entries.stream().anyMatch(entry -> entry.getRawFileEntry()
                                .getPath()
                                .toString()
                                .contains(" "));
                    }
                },
                new BrowserMenuLeafProvider() {
                    @Override
                    public KeyCombination getShortcut() {
                        return new KeyCodeCombination(
                                KeyCode.C, KeyCombination.SHIFT_DOWN, KeyCombination.SHORTCUT_DOWN);
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>(centerEllipsis(
                                    entries.getFirst()
                                            .getRawFileEntry()
                                            .getPath()
                                            .getFileName(),
                                    50));
                        }

                        return AppI18n.observable("fileNames");
                    }

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> entry.getRawFileEntry().getPath().getFileName())
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }
                },
                new BrowserMenuLeafProvider() {
                    @Override
                    public boolean automaticallyResolveLinks() {
                        return false;
                    }

                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>(centerEllipsis(
                                    entries.getFirst()
                                            .getRawFileEntry()
                                            .getPath()
                                            .getFileName(),
                                    50));
                        }

                        return AppI18n.observable("linkFileNames");
                    }

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry -> entry.getRawFileEntry().getPath().getFileName())
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }

                    @Override
                    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return entries.stream()
                                        .allMatch(browserEntry ->
                                                browserEntry.getRawFileEntry().getKind() == FileKind.LINK)
                                && entries.stream().anyMatch(browserEntry -> !browserEntry
                                        .getFileName()
                                        .equals(browserEntry
                                                .getRawFileEntry()
                                                .resolved()
                                                .getPath()
                                                .getFileName()));
                    }
                },
                new BrowserMenuLeafProvider() {
                    @Override
                    public ObservableValue<String> getName(
                            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        if (entries.size() == 1) {
                            return new SimpleObjectProperty<>("\""
                                    + centerEllipsis(
                                            entries.getFirst()
                                                    .getRawFileEntry()
                                                    .getPath()
                                                    .getFileName(),
                                            50) + "\"");
                        }

                        return AppI18n.observable("fileNamesQuoted");
                    }

                    @Override
                    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        var s = entries.stream()
                                .map(entry ->
                                        "\"" + entry.getRawFileEntry().getPath().getFileName() + "\"")
                                .collect(Collectors.joining("\n"));
                        ClipboardHelper.copyText(s);
                    }

                    @Override
                    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
                        return entries.stream().anyMatch(entry -> entry.getRawFileEntry()
                                .getPath()
                                .getFileName()
                                .contains(" "));
                    }
                });
    }
}
