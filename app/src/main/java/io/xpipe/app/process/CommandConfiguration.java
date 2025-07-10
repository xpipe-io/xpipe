package io.xpipe.app.process;

public interface CommandConfiguration {

    String rawCommand();

    String fullCommand(ShellControl shellControl);

    CommandConfiguration withRawCommand(String newCommand);
}
