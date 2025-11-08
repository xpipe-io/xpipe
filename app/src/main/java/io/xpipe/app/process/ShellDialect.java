package io.xpipe.app.process;

import io.xpipe.app.ext.FileEntry;
import io.xpipe.app.ext.FileSystem;
import io.xpipe.core.FilePath;
import io.xpipe.core.StreamCharset;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface ShellDialect {

    default boolean isMarkerDialect() {
        return false;
    }

    String unsetEnvironmentVariableCommand(String var);

    CommandBuilder launchAsnyc(CommandBuilder cmd);

    default String getLicenseFeatureId() {
        return null;
    }

    String terminalLauncherScript(UUID request, String name, boolean alwaysPromptRestart);

    String getExecutableName();

    default boolean isCompatibleTo(ShellDialect other) {
        return this.equals(other);
    }

    String getCatchAllVariable();

    String queryVersion(ShellControl shellControl) throws Exception;

    CommandControl queryFileSize(ShellControl shellControl, String file);

    long queryDirectorySize(ShellControl shellControl, String file) throws Exception;

    CommandControl prepareUserTempDirectory(ShellControl shellControl, String directory);

    FilePath getInitFileName(ShellControl sc, int hash) throws Exception;

    CommandControl directoryExists(ShellControl shellControl, String directory);

    CommandControl evaluateExpression(ShellControl shellControl, String s);

    CommandControl resolveDirectory(ShellControl shellControl, String directory);

    String literalArgument(String s);

    String prepareEnvironmentForCustomTerminalScripts();

    String fileArgument(String s);

    default String fileArgument(FilePath s) {
        return fileArgument(s.toString());
    }

    String quoteArgument(String s);

    String prepareTerminalEnvironmentCommands();

    String addToPathVariableCommand(List<String> entries, boolean append);

    default String applyInitFileCommand(ShellControl sc) throws Exception {
        return null;
    }

    String changeTitleCommand(String newTitle);

    CommandControl createStreamFileWriteCommand(ShellControl shellControl, String file, long totalBytes)
            throws Exception;

    default String getCdCommand(String directory) {
        return "cd \"" + directory + "\"";
    }

    String getScriptFileEnding();

    String assembleCommand(String command, Map<String, String> variables);

    Stream<FileEntry> listFiles(FileSystem fs, ShellControl control, String path, boolean sub) throws Exception;

    Stream<String> listRoots(ShellControl control) throws Exception;

    String getPauseCommand();

    String prepareScriptContent(ShellControl sc, String content);

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

    String getDiscardStdoutOperator();

    String getDiscardAllOperator();

    String nullStdin(String command);

    ShellDialectAskpass getAskpass();

    String getSetEnvironmentVariableCommand(String variable, String value);

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

    String terminalInitCommand(ShellControl shellControl, String file, boolean exit);

    String runScriptCommand(ShellControl parent, String file);

    String sourceScriptCommand(ShellControl sc, String file);

    String executeCommandWithShell(String cmd);

    String getMkdirsCommand(String dirs);

    CommandControl getFileReadCommand(ShellControl parent, String file);

    String getPrintWorkingDirectoryCommand();

    StreamCharset getTextCharset();

    default StreamCharset getScriptCharset() {
        return getTextCharset();
    }

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

    CommandControl createFileExistsCommand(ShellControl sc, String file) throws Exception;

    CommandControl symbolicLink(ShellControl sc, String linkFile, String targetFile);

    CommandControl getFileDeleteCommand(ShellControl parent, String file);

    CommandControl getFileTouchCommand(ShellControl parent, String file);

    String whichCommand(ShellControl sc, String executable) throws Exception;

    Charset determineCharset(ShellControl control) throws Exception;

    NewLine getNewLine();

    String getId();

    String getDisplayName();

    boolean doesEchoInputByDefault();
}
