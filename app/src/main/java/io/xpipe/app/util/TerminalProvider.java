package io.xpipe.app.util;

import io.xpipe.extension.util.ModuleLayerLoader;
import io.xpipe.extension.util.ScriptHelper;

import java.util.ServiceLoader;

public abstract class TerminalProvider {

    private static TerminalProvider INSTANCE;

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ServiceLoader.load(layer, TerminalProvider.class).findFirst().orElseThrow();
        }

        @Override
        public boolean requiresFullDaemon() {
            return true;
        }

        @Override
        public boolean prioritizeLoading() {
            return false;
        }
    }

    public static void open(String title, String command) throws Exception {
        if (command.contains("\n")) {
            command = ScriptHelper.createLocalExecScript(command);
        }

        INSTANCE.openInTerminal(title, command);
    }

    protected  abstract void openInTerminal(String title, String command) throws Exception;
}
