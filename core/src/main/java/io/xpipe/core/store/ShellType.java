package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.core.charsetter.NewLine;

import java.nio.charset.Charset;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ShellType {

    void elevate(ShellProcessControl control, String command, String displayCommand) throws Exception;

    default String getExitCommand() {
        return "exit";
    }

    String getExitCodeVariable();

    default String getConcatenationOperator() {
        return ";";
    }

    default String getAndConcatenationOperator() {
        return "&&";
    }

    String getEchoCommand(String s, boolean toErrorStream);

    String queryShellProcessId(ShellProcessControl control) throws Exception;

    List<String> openCommand();

    String switchTo(String cmd);

    List<String> createMkdirsCommand(String dirs);

    List<String> createFileReadCommand(String file);

    List<String> createFileWriteCommand(String file);

    List<String> createFileExistsCommand(String file);

    Charset determineCharset(ShellProcessControl control) throws Exception;

    NewLine getNewLine();

    String getName();

    String getDisplayName();

    boolean echoesInput();
}
