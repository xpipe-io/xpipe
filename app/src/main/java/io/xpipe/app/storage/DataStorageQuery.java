package io.xpipe.app.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class DataStorageQuery {

    public static List<DataStoreEntry> queryUserInput(String connection) {
        var found = queryEntry("**", "**" + connection + "**", "*");
        if (found.size() > 1) {
            var narrow = found.stream()
                    .filter(dataStoreEntry -> dataStoreEntry.getName().equalsIgnoreCase(connection))
                    .toList();
            if (narrow.size() == 1) {
                return narrow;
            }
        }
        return found;
    }


    public static List<DataStoreCategory> queryCategory(String categoryFilter) {
        if (DataStorage.get() == null) {
            return List.of();
        }

        var catMatcher = Pattern.compile(toRegex(categoryFilter.toLowerCase()));

        List<DataStoreCategory> found = new ArrayList<>();
        for (DataStoreCategory cat : DataStorage.get().getStoreCategories()) {
            var c = DataStorage.get().getStorePath(cat).toString();
            if (!catMatcher.matcher(c).matches()) {
                continue;
            }

            found.add(cat);
        }
        return found;
    }

    public static List<DataStoreEntry> queryEntry(String categoryFilter, String connectionFilter, String typeFilter) {
        if (DataStorage.get() == null) {
            return List.of();
        }

        var catMatcher = Pattern.compile(toRegex(categoryFilter.toLowerCase()));
        var conMatcher = Pattern.compile(toRegex(connectionFilter.toLowerCase()));
        var typeMatcher = Pattern.compile(toRegex(typeFilter.toLowerCase()));

        List<DataStoreEntry> found = new ArrayList<>();
        for (DataStoreEntry storeEntry : DataStorage.get().getStoreEntries()) {
            if (!storeEntry.getValidity().isUsable()) {
                continue;
            }

            var name = DataStorage.get().getStorePath(storeEntry).toString();
            if (!conMatcher.matcher(name).matches()) {
                continue;
            }

            var cat = DataStorage.get()
                    .getStoreCategoryIfPresent(storeEntry.getCategoryUuid())
                    .orElse(null);
            if (cat == null) {
                continue;
            }

            var c = DataStorage.get().getStorePath(cat).toString();
            if (!catMatcher.matcher(c).matches()) {
                continue;
            }

            if (!typeMatcher
                    .matcher(storeEntry.getProvider().getId().toLowerCase())
                    .matches()) {
                continue;
            }

            found.add(storeEntry);
        }
        return found;
    }

    private static String toRegex(String pattern) {
        pattern = pattern.replaceAll("\\*\\*", "#");
        // https://stackoverflow.com/a/17369948/6477761
        StringBuilder sb = new StringBuilder(pattern.length());
        int inGroup = 0;
        int inClass = 0;
        int firstIndexInClass = -1;
        char[] arr = pattern.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            switch (ch) {
                case '\\':
                    if (++i >= arr.length) {
                        sb.append('\\');
                    } else {
                        char next = arr[i];
                        switch (next) {
                            case ',':
                                // escape not needed
                                break;
                            case 'Q':
                            case 'E':
                                // extra escape needed
                                sb.append('\\');
                            default:
                                sb.append('\\');
                        }
                        sb.append(next);
                    }
                    break;
                case '*':
                    if (inClass == 0) sb.append("[^/]*");
                    else sb.append('*');
                    break;
                case '#':
                    if (inClass == 0) sb.append(".*");
                    else sb.append('*');
                    break;
                case '?':
                    if (inClass == 0) sb.append('.');
                    else sb.append('?');
                    break;
                case '[':
                    inClass++;
                    firstIndexInClass = i + 1;
                    sb.append('[');
                    break;
                case ']':
                    inClass--;
                    sb.append(']');
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    if (inClass == 0 || (firstIndexInClass == i && ch == '^')) sb.append('\\');
                    sb.append(ch);
                    break;
                case '!':
                    if (firstIndexInClass == i) sb.append('^');
                    else sb.append('!');
                    break;
                case '{':
                    inGroup++;
                    sb.append('(');
                    break;
                case '}':
                    inGroup--;
                    sb.append(')');
                    break;
                case ',':
                    if (inGroup > 0) sb.append('|');
                    else sb.append(',');
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }
}
