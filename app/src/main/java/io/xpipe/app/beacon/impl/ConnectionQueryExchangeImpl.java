package io.xpipe.app.beacon.impl;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.beacon.api.ConnectionQueryExchange;
import io.xpipe.core.store.StorePath;

import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ConnectionQueryExchangeImpl extends ConnectionQueryExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) {
        var catMatcher = Pattern.compile(toRegex("all connections/" + msg.getCategoryFilter()));
        var conMatcher = Pattern.compile(toRegex(msg.getConnectionFilter()));
        var typeMatcher = Pattern.compile(toRegex(msg.getTypeFilter()));

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

        var mapped = new ArrayList<QueryResponse>();
        for (DataStoreEntry e : found) {
            var names = DataStorage.get()
                    .getStorePath(DataStorage.get()
                            .getStoreCategoryIfPresent(e.getCategoryUuid())
                            .orElseThrow())
                    .getNames();
            var cat = new StorePath(names.subList(1, names.size()));
            var obj = ConnectionQueryExchange.QueryResponse.builder()
                    .connection(e.getUuid())
                    .category(cat)
                    .name(DataStorage.get().getStorePath(e))
                    .type(e.getProvider().getId())
                    .build();
            mapped.add(obj);
        }
        return Response.builder().found(mapped).build();
    }

    private String toRegex(String pattern) {
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
