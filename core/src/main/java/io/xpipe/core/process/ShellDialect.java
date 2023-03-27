package io.xpipe.core.process;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.store.FileSystem;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ShellDialect {

    CommandControl directoryExists(ShellControl shellControl,  String directory);

    CommandControl normalizeDirectory(ShellControl shellControl,  String directory);

    String fileArgument(String s);

    default String applyRcFileCommand() {
        return null;
    }

    CommandControl createStreamFileWriteCommand(ShellControl shellControl, String file);

    default String getCdCommand(String directory){
        return "cd \"" + directory + "\"";
    }

    default String getPushdCommand(String directory){
        return "pushd \"" + directory + "\"";
    }

    default String getPopdCommand(){
        return "popd";
    }

    String getScriptFileEnding();

    String addInlineVariablesToCommand(Map<String, String> variables, String command);

    Stream<FileSystem.FileEntry> listFiles(FileSystem fs, ShellControl control, String dir) throws Exception;

    Stream<String> listRoots(ShellControl control) throws Exception;

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

    default String getExitCommand() {
        return "exit";
    }

    String environmentVariable(String name);

    default String getConcatenationOperator() {
        return ";";
    }

    default String getOrConcatenationOperator() {
        return "||";
    }

    String getMakeExecutableCommand(String file);

    default String getSelfdeleteEchoScriptContent(String s) {
        return getEchoCommand(s, false);
    }

    String getSetEnvironmentVariableCommand(String variable, String value);

    String getEchoCommand(String s, boolean toErrorStream);

    String getPrintVariableCommand(String name);

    String getPrintExitCodeCommand(String prefix);

    default String getPrintEnvironmentVariableCommand(String name) {
        return getPrintVariableCommand(name);
    }

    String getOpenCommand();

    String prepareTerminalInitFileOpenCommand(ShellDialect parentDialect, ShellControl sc, String file) throws Exception;

    String runScript(String file);

    String sourceScript(String file);

    String executeCommandWithShell(String cmd);

    String getMkdirsCommand(String dirs);

    String getFileReadCommand(String file);

    String getPrintWorkingDirectoryCommand();

    String getFileCopyCommand(String oldFile, String newFile);

    String getFileMoveCommand(String oldFile, String newFile);

    default boolean requiresScript(String content) {
        return content.contains("\n");
    }

    CommandControl createTextFileWriteCommand(ShellControl parent, String content, String file);

    String getFileDeleteCommand(String file);

    String getFileExistsCommand(String file);

    String getFileTouchCommand(String file);

    String getWhichCommand(String executable);

    Charset determineCharset(ShellControl control) throws Exception;

    NewLine getNewLine();

    String getId();

    String getDisplayName();

    boolean doesEchoInput();
}
