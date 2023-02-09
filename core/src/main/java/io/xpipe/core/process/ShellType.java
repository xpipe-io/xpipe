package io.xpipe.core.process;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.core.charsetter.NewLine;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
public interface ShellType {

    String getScriptFileEnding();

    String addInlineVariablesToCommand(Map<String, String> variables, String command);

    String getPauseCommand();

    String prepareScriptContent(String content);

    default String flatten(List<String> command) {
        return command.stream()
                .map(s -> s.contains(" ")
                        && !(s.startsWith("\"") && s.endsWith("\""))
                        && !(s.startsWith("'") && s.endsWith("'"))
                        ? "\"" + s + "\""
                        : s)
                .collect(Collectors.joining(" "));
    }

    void disableHistory(ShellProcessControl pc) throws Exception;

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

     String getSetEnvironmentVariableCommand(String variable, String value);


    String getEchoCommand(String s, boolean toErrorStream);


    String getPrintVariableCommand(String name);

    String getPrintExitCodeCommand(String prefix);

    default String getPrintEnvironmentVariableCommand(String name) {
        return getPrintVariableCommand(name);
    }

    String getNormalOpenCommand();

    String getInitFileOpenCommand(String file);

    String executeCommandWithShell(String cmd);

    List<String> executeCommandListWithShell(String cmd);

    List<String> executeCommandListWithShell(List<String> cmd);

    List<String> getMkdirsCommand(String dirs);

    String getFileReadCommand(String file);

    String getStreamFileWriteCommand(String file);

    String getTextFileWriteCommand(String content, String file);

    String getFileDeleteCommand(String file);

    String getFileExistsCommand(String file);

    String getFileTouchCommand(String file);

    String getWhichCommand(String executable);

    Charset determineCharset(ShellProcessControl control) throws Exception;

    NewLine getNewLine();

    String getName();

    String getDisplayName();

    String getExecutable();

    boolean doesRepeatInput();
}
