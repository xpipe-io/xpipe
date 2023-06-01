package io.xpipe.core.process;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.store.FileSystem;
import io.xpipe.core.util.SecretValue;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ShellDialect {

    String initFileName(ShellControl sc) throws Exception;

    CommandControl directoryExists(ShellControl shellControl,  String directory);

    CommandControl normalizeDirectory(ShellControl shellControl,  String directory);

    String fileArgument(String s);

    String executeWithNoInitFiles(ShellDialect parentDialect, String file);

    void prepareDumbTerminalCommands(ShellControl sc) throws Exception;

    String prepareProperTerminalCommands();

    default String applyRcFileCommand() {
        return null;
    }

    String changeTitleCommand(String newTitle);

    default String applyProfileFilesCommand() {
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

    String prepareAskpassContent(ShellControl sc, String fileName, List<String> s) throws Exception;

    String getSetEnvironmentVariableCommand(String variable, String value);

    String setSecretEnvironmentVariableCommand(ShellControl sc, String variable, SecretValue value) throws Exception;

    String getEchoCommand(String s, boolean toErrorStream);

    String getPrintVariableCommand(String name);

    String getUsernameVariableName();

    String getPrintExitCodeCommand(String prefix);

    default String getPrintEnvironmentVariableCommand(String name) {
        return getPrintVariableCommand(name);
    }

    String getOpenCommand();

    String prepareTerminalInitFileOpenCommand(ShellDialect parentDialect, ShellControl sc, String file) throws Exception;

    String runScript(ShellControl parent, String file);

    String sourceScript(String file);

    String executeCommandWithShell(String cmd);

    String getMkdirsCommand(String dirs);

    String getFileReadCommand(String file);

    String getPrintWorkingDirectoryCommand();

    StreamCharset getScriptCharset();

    String getFileCopyCommand(String oldFile, String newFile);

    String getFileMoveCommand(String oldFile, String newFile);

    default boolean requiresScript(String content) {
        return content.contains("\n");
    }

    CommandControl createTextFileWriteCommand(ShellControl parent, String content, String file);

    CommandControl createScriptTextFileWriteCommand(ShellControl parent, String content, String file);

    String getFileDeleteCommand(String file);

    CommandControl createFileExistsCommand(ShellControl sc, String file);

    String getFileTouchCommand(String file);

    String getWhichCommand(String executable);

    Charset determineCharset(ShellControl control) throws Exception;

    NewLine getNewLine();

    String getId();

    String getDisplayName();

    boolean doesEchoInput();
}
