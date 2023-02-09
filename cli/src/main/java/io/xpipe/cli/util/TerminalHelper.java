package io.xpipe.cli.util;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class TerminalHelper {

    private static Terminal systemTerminal;
    private static boolean dumb;

    public static Terminal init() {
        try {
            systemTerminal = TerminalBuilder.builder().system(true).dumb(true).build();
            dumb = systemTerminal.getType().equals(Terminal.TYPE_DUMB)
                    || systemTerminal.getType().equals(Terminal.TYPE_DUMB_COLOR);
            return systemTerminal;
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean isDumb() {
        return dumb;
    }

    public static Terminal get() {
        return systemTerminal;
    }
}
