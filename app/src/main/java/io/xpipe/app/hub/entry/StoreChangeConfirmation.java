package io.xpipe.app.hub.entry;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StoreChangeConfirmation {

    public static boolean confirmDelete(List<DataStoreEntry> entries) {
        // Children are deleted as well, they might be in other categories
        var affected = entries.stream()
                .flatMap(e -> {
                    var all = new ArrayList<DataStoreEntry>(DataStorage.get().getDeepStoreChildren(e));
                    all.addFirst(e);
                    return all.stream();
                })
                .distinct()
                .filter(e -> DataStorage.get().requiresChangeConfirmation(e))
                .toList();
        if (affected.isEmpty()) {
            return true;
        }

        return AppDialog.confirm(
                "confirmConnectionChange", AppI18n.observable("confirmConnectionDeleteContent", formatEntries(affected)));
    }

    public static boolean confirmMove(List<DataStoreEntry> entries, DataStoreCategory target) {
        var affected = entries.stream()
                .filter(e -> !e.getCategoryUuid().equals(target.getUuid()))
                .filter(e -> DataStorage.get().requiresChangeConfirmation(e)
                        || DataStorage.get().requiresChangeConfirmation(target))
                .toList();
        if (affected.isEmpty()) {
            return true;
        }

        return AppDialog.confirm(
                "confirmConnectionChange",
                AppI18n.observable("confirmConnectionMoveContent", target.getName(), formatEntries(affected)));
    }

    public static boolean confirmRename(DataStoreEntry entry, String newName) {
        if (!DataStorage.get().requiresChangeConfirmation(entry)) {
            return true;
        }

        return AppDialog.confirm(
                "confirmConnectionChange",
                AppI18n.observable("confirmConnectionRenameContent", newName, formatEntries(List.of(entry))));
    }

    public static boolean confirmEdit(DataStoreEntry entry) {
        if (!DataStorage.get().requiresChangeConfirmation(entry)) {
            return true;
        }

        return AppDialog.confirm(
                "confirmConnectionChange",
                AppI18n.observable("confirmConnectionEditContent", formatEntries(List.of(entry))));
    }

    private static String formatEntries(List<DataStoreEntry> entries) {
        return entries.stream().map(e -> "- " + e.getName()).collect(Collectors.joining("\n"));
    }
}
