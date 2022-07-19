package io.xpipe.core.store;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

public interface StandardShellStore extends ShellStore {

    static interface ShellType {

        List<String> switchTo(List<String> cmd);

        default ProcessControl prepareElevatedCommand(ShellStore st, InputStream in, List<String> cmd, String pw) throws Exception {
            return st.prepareCommand(in, cmd);
        }

        List<String> createFileReadCommand(String file);

        List<String> createFileWriteCommand(String file);

        List<String> createFileExistsCommand(String file);

        Charset getCharset();

        String getName();

        String getDisplayName();
    }

    ShellType determineType() throws Exception;
}
