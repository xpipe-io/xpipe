package io.xpipe.core.process;

import io.xpipe.core.store.FilePath;
import io.xpipe.core.store.FileSystem;
import io.xpipe.core.util.NewLine;
import io.xpipe.core.util.SecretValue;
import io.xpipe.core.util.StreamCharset;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface ShellDialect {

    CommandBuilder launchAsnyc(CommandBuilder cmd);

    default String getLicenseFeatureId() {
        return null;
    }

    String terminalLauncherScript(UUID request, String name);

    String getExecutableName();

    default boolean isSelectable() {
        return true;
    }

    default boolean isCompatibleTo(ShellDialect other) {
        return other.equals(this);
    }

    String getCatchAllVariable();

    String queryVersion(ShellControl shellControl) throws Exception;

    CommandControl queryFileSize(ShellControl shellControl, String file);

    CommandControl prepareUserTempDirectory(ShellControl shellControl, String directory);

    String initFileName(ShellControl sc) throws Exception;

    CommandControl directoryExists(ShellControl shellControl, String directory);

    CommandControl evaluateExpression(ShellControl shellControl, String s);

    CommandControl resolveDirectory(ShellControl shellControl, String directory);

    String literalArgument(String s);

    String fileArgument(String s);

    default String fileArgument(FilePath s) {
        return fileArgument(s.toString());
    }

    String quoteArgument(String s);

    String prepareTerminalEnvironmentCommands();

    String addToPathVariableCommand(List<String> entries, boolean append);

    default String applyInitFileCommand() {
        return null;
    }

    String changeTitleCommand(String newTitle);

    CommandControl createStreamFileWriteCommand(ShellControl shellControl, String file, long totalBytes);

    default String getCdCommand(String directory) {
        return "cd \"" + directory + "\"";
    }

    String getScriptFileEnding();

    String assembleCommand(String command, Map<String, String> variables);

    Stream<FileSystem.FileEntry> listFiles(FileSystem fs, ShellControl control, String dir) throws Exception;

    Stream<String> listRoots(ShellControl control) throws Exception;

    String getPauseCommand();

    String prepareScriptContent(String content);

    default String getPassthroughExitCommand() {
        return "exit";
    }

    default String getNormalExitCommand() {
        return "exit 0";
    }

    String environmentVariable(String name);

    default String getConcatenationOperator() {
        return ";";
    }

    String getDiscardOperator();

    String nullStdin(String command);

    String getScriptPermissionsCommand(String file);

    ShellDialectAskpass getAskpass();

    String getSetEnvironmentVariableCommand(String variable, String value);

    String setSecretEnvironmentVariableCommand(ShellControl sc, String variable, SecretValue value) throws Exception;

    String getEchoCommand(String s, boolean toErrorStream);

    String getPrintVariableCommand(String name);

    CommandControl printUsernameCommand(ShellControl shellControl);

    String getPrintStartEchoCommand(String prefix);

    Optional<String> executeRobustBootstrapOutputCommand(ShellControl shellControl, String original) throws Exception;

    String getPrintExitCodeCommand(String id, String prefix, String suffix);

    int assignMissingExitCode();

    default String getPrintEnvironmentVariableCommand(String name) {
        return getPrintVariableCommand(name);
    }

    CommandBuilder getOpenScriptCommand(String file);

    default void prepareCommandForShell(CommandBuilder b) {}

    String prepareTerminalInitFileOpenCommand(ShellDialect parentDialect, ShellControl sc, String file, boolean exit);

    String runScriptCommand(ShellControl parent, String file);

    String sourceScriptCommand(ShellControl parent, String file);

    String executeCommandWithShell(String cmd);

    String getMkdirsCommand(String dirs);

    CommandControl getFileReadCommand(ShellControl parent, String file);

    String getPrintWorkingDirectoryCommand();

    StreamCharset getScriptCharset();

    CommandControl getFileCopyCommand(ShellControl parent, String oldFile, String newFile);

    CommandControl getFileMoveCommand(ShellControl parent, String oldFile, String newFile);

    default boolean requiresScript(String content) {
        return content.contains("\n");
    }

    CommandControl createTextFileWriteCommand(ShellControl parent, String content, String file);

    CommandControl createScriptTextFileWriteCommand(ShellControl parent, String content, String file);

    CommandControl deleteFileOrDirectory(ShellControl sc, String file);

    String clearDisplayCommand();

    ShellLaunchCommand getLaunchCommand();

    ShellDumbMode getDumbMode();

    CommandControl createFileExistsCommand(ShellControl sc, String file);

    CommandControl symbolicLink(ShellControl sc, String linkFile, String targetFile);

    CommandControl getFileDeleteCommand(ShellControl parent, String file);

    CommandControl getFileTouchCommand(ShellControl parent, String file);

    String getWhichCommand(String executable);

    Charset determineCharset(ShellControl control);

    NewLine getNewLine();

    String getId();

    String getDisplayName();

    boolean doesEchoInputByDefault();
}
