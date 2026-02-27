package io.xpipe.app.cred;

import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.storage.DataStoreEntryRef;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SshAgentKeyList {

    @Value
    public static class Entry {

        String type;
        String publicKey;
        String name;
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
