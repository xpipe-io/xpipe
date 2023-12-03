package io.xpipe.core.process;

public interface CommandConfiguration {

    String rawCommand();

    String fullCommand(ShellControl shellControl) throws Exception;

    CommandConfiguration withRawCommand(String newCommand);
}
