package io.xpipe.ext.proc;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.store.MachineStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.util.Validators;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuperBuilder
@Jacksonized
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@JsonTypeName("wsl")
public class WslStore extends JacksonizedValue implements MachineStore {

    ShellStore host;
    String distribution;
    String user;

    public WslStore(ShellStore host, String distribution, String user) {
        this.host = host;
        this.distribution = distribution;
        this.user = user;
    }

    public static boolean isSupported(ShellStore host) {
        return ShellStore.local()
                .create()
                .command("wsl --list")
                .customCharset(StandardCharsets.UTF_16LE)
                .startAndCheckExit();
    }

    public static Optional<String> getDefaultDistribution(ShellStore host) {
        String s = null;
        try (var pc = host.create()
                .command("wsl --list")
                .customCharset(StandardCharsets.UTF_16LE)
                .start()) {
            s = pc.readOnlyStdout();
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
            return Optional.empty();
        }

        var def = s.lines()
                .skip(1)
                .filter(line -> {
                    return line.trim().endsWith("(Default)");
                })
                .findFirst();
        return def.map(line -> line.replace(" (Default)", ""));
    }

    @Override
    public void checkComplete() throws Exception {
        Validators.nonNull(host, "Host");
        host.checkComplete();
        Validators.nonNull(distribution, "Distribution");
        Validators.nonNull(user, "User");
    }

    @Override
    public void validate() throws Exception {
        Validators.hostFeature(host, WslStore::isSupported, "wsl");
    }

    @Override
    public ShellProcessControl create() {
        var l = createCommand();
        return host.create()
                .subShell(
                        shellProcessControl ->
                                shellProcessControl.getShellType().flatten(l),
                        (shellProcessControl, s) -> {
                            var flattened = shellProcessControl.getShellType().flatten(l);
                            if (s != null) {
                                flattened += " " + s;
                            }
                            return flattened;
                        });
    }

    private List<String> createCommand() {
        var l = new ArrayList<String>(List.of("wsl"));

        if (user != null) {
            l.add("-u");
            l.add(user);
        }

        if (distribution != null) {
            l.add("--distribution");
            l.add(distribution);
        }

        return l;
    }
}
