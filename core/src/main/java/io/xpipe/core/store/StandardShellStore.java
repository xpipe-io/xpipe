package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.util.SecretValue;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;

public interface StandardShellStore extends MachineFileStore, ShellStore {

    public default boolean isLocal() {
        return false;
    }

    public default NewLine getNewLine() throws Exception {
        return determineType().getNewLine();
    }

    public abstract ShellType determineType() throws Exception;

    public default String querySystemName() throws Exception {
        var result = executeAndCheckOut(List.of(), determineType().getOperatingSystemNameCommand(), getTimeout());
        return result.strip();
    }

    @Override
    public default InputStream openInput(String file) throws Exception {
        var type = determineType();
        var cmd = type.createFileReadCommand(file);
        var p = prepareCommand(List.of(), cmd, null);
        p.start();
        return p.getStdout();
    }

    @Override
    public default OutputStream openOutput(String file) throws Exception {
        return null;
        //        var type = determineType();
        //        var cmd = type.createFileWriteCommand(file);
        //        var p = prepare(cmd).redirectErrorStream(true);
        //        var proc = p.start();
        //        return proc.getOutputStream();
    }

    @Override
    public default boolean exists(String file) throws Exception {
        var type = determineType();
        var cmd = type.createFileExistsCommand(file);
        var p = prepareCommand(List.of(), cmd, null);
        p.start();
        return p.waitFor() == 0;
    }

    @Override
    public default void mkdirs(String file) throws Exception {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    public static interface ShellType {

        List<String> switchTo(List<String> cmd);

        default ProcessControl prepareElevatedCommand(
                ShellStore st, List<SecretValue> in, List<String> cmd, Integer timeout, String pw) throws Exception {
            return st.prepareCommand(in, cmd, timeout);
        }

        List<String> createFileReadCommand(String file);

        List<String> createFileWriteCommand(String file);

        List<String> createFileExistsCommand(String file);

        Charset determineCharset(ShellStore store) throws Exception;

        NewLine getNewLine();

        String getName();

        String getDisplayName();

        List<String> getOperatingSystemNameCommand();
    }
}
