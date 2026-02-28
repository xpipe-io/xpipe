package io.xpipe.app.cred;

import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import lombok.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SshAgentKeyList {

    @Value
    public static class Entry {

        String type;
        String publicKey;
        String name;
    }

    public static Entry findAgentIdentity(DataStoreEntryRef<ShellStore> ref, SshIdentityStrategy strategy, String identifier) throws Exception {
        var list = listAgentIdentities(ref, strategy).stream().filter(entry -> {
            return entry.getName().equalsIgnoreCase(identifier) || entry.getPublicKey().equalsIgnoreCase(identifier);
        }).toList();

        if (list.isEmpty()) {
            throw ErrorEventFactory.expected(new IllegalArgumentException("No such agent identity: " + identifier));
        }

        if (list.size() > 1) {
            throw ErrorEventFactory.expected(new IllegalArgumentException("Ambiguous agent identities: " + list.stream().map(entry -> entry.getName()).collect(
                    Collectors.joining(", "))));
        }

        return list.getFirst();
    }

    public static List<Entry> listAgentIdentities(DataStoreEntryRef<ShellStore> ref, SshIdentityStrategy strategy) throws Exception {
        var session = ref.getStore().getOrStartSession();
        strategy.prepareParent(session);

        var out = session.command(CommandBuilder.of().add("ssh-add", "-L")).readStdoutOrThrow();
        var pattern = Pattern.compile("([^ ]+) ([^ ]+) (.+)");
        var lines = out.lines().toList();
        var list = new ArrayList<Entry>();
        for (String line : lines) {
            var matcher = pattern.matcher(line);
            if (!matcher.matches()) {
                continue;
            }

            var type = matcher.group(1);
            var publicKey = matcher.group(2);
            var name = matcher.group(3);
            list.add(new Entry(type, publicKey, name));
        }
        return list;
    }
}
