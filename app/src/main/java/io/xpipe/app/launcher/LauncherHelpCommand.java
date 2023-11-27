package io.xpipe.app.launcher;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "--help",
        header = "Displays help information",
        helpCommand = true
)
public class LauncherHelpCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("""
                HELP
                
                COMMAND!
                """);
        return 0;
    }
}
