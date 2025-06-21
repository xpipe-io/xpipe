package io.xpipe.app.terminal;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.ShellTemp;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.process.ShellTerminalInitCommand;
import io.xpipe.core.store.FilePath;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface TerminalPrompt {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(StarshipTerminalPrompt.class);
        l.add(OhMyPoshTerminalPrompt.class);
        l.add(OhMyZshTerminalPrompt.class);
        return l;
    }

    String getDocsLink();

    default FilePath getConfigurationDirectory(ShellControl sc) throws Exception {
        var d = ShellTemp.createUserSpecificTempDataDirectory(sc, "prompt").join(getId());
        sc.view().mkdir(d);
        return d;
    }

    default FilePath getBinaryDirectory(ShellControl sc) throws Exception {
        var d = ShellTemp.createUserSpecificTempDataDirectory(sc, "bin").join(getId());
        sc.view().mkdir(d);
        return d;
    }

    String getId();

    default boolean installIfNeeded(ShellControl sc) throws Exception {
        if (!checkIfInstalled(sc)) {
            try {
                checkCanInstall(sc);
                install(sc);
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).omit().handle();
                return false;
            }
            return true;
        }
        return true;
    }

    void checkCanInstall(ShellControl sc) throws Exception;

    boolean checkIfInstalled(ShellControl sc) throws Exception;

    void install(ShellControl sc) throws Exception;

    ShellTerminalInitCommand terminalCommand() throws Exception;

    List<ShellDialect> getSupportedDialects();
}
