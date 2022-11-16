package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.core.charsetter.NewLine;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
public interface ShellType {

    void elevate(ShellProcessControl control, String command, String displayCommand) throws IOException;

    default void init(ProcessControl proc) throws IOException {
    }

    default String getExitCommand() {
        return "exit";
    }

    String getExitCodeVariable();

    default String getConcatenationOperator() {
        return ";";
    }

    String getEchoCommand(String s, boolean newLine);

    List<String> openCommand();
    String switchTo(String cmd);

    List<String> createMkdirsCommand(String dirs);

    List<String> createFileReadCommand(String file);

    List<String> createFileWriteCommand(String file);

    List<String> createFileExistsCommand(String file);

    Charset determineCharset(ProcessControl control) throws Exception;

    NewLine getNewLine();

    String getName();

    String getDisplayName();

    List<String> getOperatingSystemNameCommand();

    boolean echoesInput();
}
