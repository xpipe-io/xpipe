package io.xpipe.app.cred;

import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.storage.DataStoreEntryRef;

import lombok.Value;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SshAgentKeyList {

    @Value
    public static class Entry {

        String type;
        String publicKey;
        String name;

        @Override
        public String toString() {
            return type + " " + publicKey + (name != null ? " " + name : "");
        }
    }

    public static Entry findAgentIdentity(
            DataStoreEntryRef<ShellStore> ref, SshIdentityAgentStrategy strategy, String identifier) throws Exception {
        var all = listAgentIdentities(ref, strategy);
        var list = all.stream()
                .filter(entry -> {
                    return (entry.getName() != null && entry.getName().equalsIgnoreCase(identifier))
                            || entry.getPublicKey().equals(identifier)
                            || (entry.getType() + " " + entry.getPublicKey()).equals(identifier);
                })
                .toList();

        if (list.isEmpty()) {
            var noNames = all.stream().allMatch(entry -> entry.getName() == null);

            var isPublicKey = identifier.contains(" ");
            if (!isPublicKey) {
                try {
                    Base64.getDecoder().decode(identifier);
                    isPublicKey = true;
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (noNames) {
                if (!isPublicKey) {
                    throw ErrorEventFactory.expected(new IllegalArgumentException("Found no agent identity for name "
                            + identifier + " as the agent does not support names. Use a public key instead"));
                }
            }

            throw ErrorEventFactory.expected(new IllegalArgumentException(
                    "Found no agent identity for " + (isPublicKey ? "public key " : "name ") + identifier));
        }

        if (list.size() > 1) {
            throw ErrorEventFactory.expected(new IllegalArgumentException("Ambiguous agent identities: "
                    + list.stream()
                            .map(entry -> entry.getName() != null ? entry.getName() : entry.toString())
                            .collect(Collectors.joining(", "))));
        }

        return list.getFirst();
    }

    public static List<Entry> listAgentIdentities(DataStoreEntryRef<ShellStore> ref, SshIdentityAgentStrategy strategy)
            throws Exception {
        var session = ref.getStore().getOrStartSession();
        strategy.prepareParent(session);

        var socket = strategy.determinetAgentSocketLocation(session);
        var out = session.command(CommandBuilder.of()
                        .add("ssh-add", "-L")
                        .fixedEnvironment("SSH_AUTH_SOCK", socket != null ? socket.toString() : null))
                .readStdoutOrThrow();
        var pattern = Pattern.compile("([^ ]+) ([^ ]+)\\s*(?: (.+))?");
        var lines = out.lines().toList();
        var list = new ArrayList<Entry>();
        for (String line : lines) {
            var matcher = pattern.matcher(line);
            if (!matcher.matches()) {
                continue;
            }

            var type = matcher.group(1);
            var publicKey = matcher.group(2);
            var name = matcher.groupCount() > 2 ? matcher.group(3) : null;
            list.add(new Entry(type, publicKey, name));
        }
        return list;
    }
}
