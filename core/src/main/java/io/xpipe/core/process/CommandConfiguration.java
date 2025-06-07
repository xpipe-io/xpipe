package io.xpipe.core.process;

public interface CommandConfiguration {

    String rawCommand();

    String fullCommand(ShellControl shellControl);

    CommandConfiguration withRawCommand(String newCommand);
}
