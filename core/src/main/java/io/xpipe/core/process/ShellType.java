package io.xpipe.core.process;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.core.charsetter.NewLine;

import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ShellType {

    String getScriptFileEnding();

    default String commandWithVariable(String key, String value, String command) {
        return joinCommands(getSetVariableCommand(key, value), command);
    }

    String getPauseCommand();

    String createInitFileContent(String command);

    String getTerminalFileOpenCommand(String file);

    default String flatten(List<String> command) {
        return command.stream()
                .map(s -> s.contains(" ")
                                && !(s.startsWith("\"") && s.endsWith("\""))
                                && !(s.startsWith("'") && s.endsWith("'"))
                        ? "\"" + s + "\""
                        : s)
                .collect(Collectors.joining(" "));
    }

    default String joinCommands(String... s) {
        return String.join(getConcatenationOperator(), s);
    }

    void elevate(ShellProcessControl control, String command, String displayCommand) throws Exception;

    default String getExitCommand() {
        return "exit";
    }

    String getExitCodeVariable();

    default String getConcatenationOperator() {
        return ";";
    }

    default String getOrConcatenationOperator() {
        return "||";
    }

    String getMakeExecutableCommand(String file);

    default String getScriptEchoCommand(String s) {
        return getEchoCommand(s, false);
    }

    String getEchoCommand(String s, boolean toErrorStream);

    String queryShellProcessId(ShellProcessControl control) throws Exception;

    String getSetVariableCommand(String variableName, String value);

    default String getPrintVariableCommand(String name) {
        return getPrintVariableCommand("", name);
    }

    String getPrintVariableCommand(String prefix, String name);

    String getNormalOpenCommand();

    String executeCommandWithShell(String cmd);

    List<String> createMkdirsCommand(String dirs);

    String createFileReadCommand(String file);

    String createFileWriteCommand(String file);

    String createFileDeleteCommand(String file);

    String createFileExistsCommand(String file);

    String createWhichCommand(String executable);

    Charset determineCharset(ShellProcessControl control) throws Exception;

    NewLine getNewLine();

    String getName();

    String getDisplayName();

    String getExecutable();

    boolean echoesInput();
}
