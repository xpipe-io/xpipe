package io.xpipe.ext.proc;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellTypes;
import io.xpipe.core.store.MachineStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.extension.util.Validators;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@JsonTypeName("docker")
@SuperBuilder
@Jacksonized
@Getter
public class DockerStore extends JacksonizedValue implements MachineStore {

    private final ShellStore host;
    private final String containerName;

    public DockerStore(ShellStore host, String containerName) {
        this.host = host;
        this.containerName = containerName;
    }

    public static boolean isSupported(ShellStore host) {
        return host.create().command("docker --help").startAndCheckExit();
    }

    @Override
    public void checkComplete() throws Exception {
        Validators.nonNull(host, "Host");
        host.checkComplete();
        Validators.nonNull(containerName, "Name");
    }

    @Override
    public void validate() throws Exception {
        host.validate();
        Validators.hostFeature(host, DockerStore::isSupported, "docker");

        try (var pc = host.create()
                .command(List.of("docker", "container", "inspect", containerName))
                .elevated()
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public ShellProcessControl create() {
        return host.create()
                .subShell(
                        shellProcessControl ->
                                "docker exec -i " + containerName + " " + ShellTypes.BASH.getNormalOpenCommand(),
                        (shellProcessControl, s) -> {
                            if (s != null) {
                                return "docker exec -it " + containerName + " "
                                        + ShellTypes.BASH.executeCommandWithShell(s);
                            } else {
                                return "docker exec -it " + containerName + " "
                                        + ShellTypes.BASH.getNormalOpenCommand();
                            }
                        })
                .elevated(shellProcessControl -> true);
    }
}
