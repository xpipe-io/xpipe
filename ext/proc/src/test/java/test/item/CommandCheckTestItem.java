package test.item;

import io.xpipe.core.process.CommandProcessControl;
import io.xpipe.core.process.ShellProcessControl;
import lombok.Getter;
import org.apache.commons.lang3.function.FailableConsumer;
import org.junit.jupiter.api.Assertions;

import java.util.function.Function;

@Getter
public enum CommandCheckTestItem {
    ECHO(
            shellProcessControl -> {
                return shellProcessControl.getShellType().getEchoCommand("hi", false);
            },
            commandProcessControl -> {
                Assertions.assertEquals("hi", commandProcessControl.readOrThrow());
            });

    private final Function<ShellProcessControl, String> commandFunction;
    private final FailableConsumer<CommandProcessControl, Exception> commandCheck;

    CommandCheckTestItem(
            Function<ShellProcessControl, String> commandFunction,
            FailableConsumer<CommandProcessControl, Exception> commandCheck) {
        this.commandFunction = commandFunction;
        this.commandCheck = commandCheck;
    }
}
