package io.xpipe.app.terminal;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.app.util.ShellTemp;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.process.ShellTerminalInitCommand;
import io.xpipe.core.store.FilePath;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface TerminalPrompt {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(StarshipTerminalPrompt.class);
        return l;
    }

    String getDocsLink();

    default FilePath getConfigurationDirectory(ShellControl sc) throws Exception {
        return ShellTemp.createUserSpecificTempDataDirectory(sc, "prompt");
    }

    default FilePath getBinaryDirectory(ShellControl sc) throws Exception {
        return ShellTemp.createUserSpecificTempDataDirectory(sc, "bin");
    }

    default void installIfNeeded(ShellControl sc) throws Exception {
        if (checkIfInstalled(sc)) {
            checkCanInstall(sc);
            install(sc);
        }
    }

    void checkCanInstall(ShellControl sc) throws Exception;

    boolean checkIfInstalled(ShellControl sc) throws Exception;

    void install(ShellControl sc) throws Exception;

    ShellTerminalInitCommand terminalCommand(ShellControl shellControl) throws Exception;

    List<ShellDialect> getSupportedDialects();
}
