package io.xpipe.core.process;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.charsetter.NewLine;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ShellTypes {

    public static final ShellType POWERSHELL = new PowerShell();
    public static final ShellType CMD = new Cmd();
    public static final ShellType SH = new Sh();
    public static final ShellType BASH = new Bash();
    public static final ShellType ZSH = new Zsh();

    public static ShellType getPlatformDefault() {
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            return CMD;
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            return BASH;
        } else {
            return ZSH;
        }
    }

    public static ShellType[] getAllShellTypes() {
        return new ShellType[]{
                CMD,
                POWERSHELL,
                ZSH,
                BASH,
                SH
        };
    }

    @JsonTypeName("cmd")
    @Value
    public static class Cmd implements ShellType {

        @Override
        public String addInlineVariablesToCommand(Map<String, String> variables, String command) {
            var content = "";
            for (Map.Entry<String, String> e : variables.entrySet()) {
                content += ("set \"" + e.getKey() + "=" + e.getValue().replaceAll("\"", "^$0") + "\"");
                content += getConcatenationOperator();
            }
            return content + command;
        }

        @Override
        public String getSetEnvironmentVariableCommand(String variableName, String value) {
            return ("set \"" + variableName + "=" + value.replaceAll("\"", "^$0") + "\"");
        }

        @Override
        public String getPrintVariableCommand(String prefix, String name) {
            return "call echo " + prefix + "^%" + name + "^%";
        }

        @Override
        public String getEchoCommand(String s, boolean toErrorStream) {
            return "(echo " + escapeStringValue(s).replaceAll("\r\n", "^\r\n") + (toErrorStream ? ")1>&2" : ")");
        }

        @Override
        public String getScriptEchoCommand(String s) {
            return ("set \"echov=" + escapeStringValue(s)
                    + "\"\r\necho %echov%\r\n@echo on\r\n(goto) 2>nul & del \"%~f0\"");
        }

        @Override
        public String getConcatenationOperator() {
            return "&";
        }

        @Override
        public String getMakeExecutableCommand(String file) {
            return "echo.";
        }

        public String escapeStringValue(String input) {
            return input.replaceAll("[&^|<>\"]", "^$0");
        }

        @Override
        public String getScriptFileEnding() {
            return "bat";
        }

        @Override
        public String getPauseCommand() {
            return "pause";
        }

        @Override
        public String prepareScriptContent(String content) {
            return "@echo off\r\n" + content;
        }

        @Override
        public void disableHistory(ShellProcessControl pc) throws IOException {
        }

        @Override
        public String getExitCodeVariable() {
            return "errorlevel";
        }

        @Override
        public NewLine getNewLine() {
            return NewLine.CRLF;
        }

        @Override
        public String getNormalOpenCommand() {
            return "cmd";
        }

        @Override
        public String getInitFileOpenCommand(String file) {
            return "cmd /K \"" + file + "\"";
        }

        @Override
        public String executeCommandWithShell(String cmd) {
            return "cmd.exe /C " + cmd + "";
        }

        @Override
        public List<String> executeCommandListWithShell(String cmd) {
            return List.of("cmd", "/C", cmd);
        }

        @Override
        public List<String> getMkdirsCommand(String dirs) {
            return List.of("(", "if", "not", "exist", dirs, "mkdir", dirs, ")");
        }

        @Override
        public String getFileReadCommand(String file) {
            return "type \"" + file + "\"";
        }

        @Override
        public String getStreamFileWriteCommand(String file) {
            return "findstr \"^\" > \"" + file + "\"";
        }

        @Override
        public String getTextFileWriteCommand(String content, String file) {
            // if (true) return getEchoCommand(content, false) + " > \"" + file + "\"";

            var command = new ArrayList<String>();
            for (String line : content.split("(\n|\r\n)")) {
                var echoCommand = line.isEmpty() ? "echo." : "echo " + escapeStringValue(line);
                command.add(echoCommand + ">> \"" + file + "\"");
            }
            return String.join("&", command).replaceFirst(">>", ">");
        }

        @Override
        public String getFileDeleteCommand(String file) {
            return "rd /s /q \"" + file + "\"";
        }

        @Override
        public String getFileExistsCommand(String file) {
            return String.format("dir /a \"%s\"", file);
        }

        @Override
        public String getFileTouchCommand(String file) {
            return "COPY NUL \"" + file + "\"";
        }

        @Override
        public String getWhichCommand(String executable) {
            return "where \"" + executable + "\"";
        }

        @Override
        public Charset determineCharset(ShellProcessControl control) throws Exception {
            control.writeLine("chcp");

            var r = new BufferedReader(new InputStreamReader(control.getStdout(), StandardCharsets.US_ASCII));
            // Read echo of command
            r.readLine();
            // Read actual output
            var line = r.readLine();
            // Read additional empty line
            r.readLine();

            var matcher = Pattern.compile("\\d+").matcher(line);
            matcher.find();
            return Charset.forName("ibm" + matcher.group());
        }

        @Override
        public String getName() {
            return "cmd";
        }

        @Override
        public String getDisplayName() {
            return "cmd.exe";
        }

        @Override
        public String getExecutable() {
            return "C:\\Windows\\System32\\cmd.exe";
        }

        @Override
        public boolean doesRepeatInput() {
            return true;
        }
    }

    @JsonTypeName("powershell")
    @Value
    public static class PowerShell implements ShellType {

        @Override
        public void disableHistory(ShellProcessControl pc) throws Exception {
            pc.executeLine("Set-PSReadLineOption -HistorySaveStyle SaveNothing");
        }

        @Override
        public String addInlineVariablesToCommand(Map<String, String> variables, String command) {
            var content = "";
            for (Map.Entry<String, String> e : variables.entrySet()) {
                content += "$env:" + e.getKey() + " = \"" + escapeStringValue(e.getValue()) + "\"";
                content += getConcatenationOperator();
            }
            return content + command;
        }

        @Override
        public String getTextFileWriteCommand(String content, String file) {
            return "echo \"" + content.replaceAll("(\n|\r\n)", "`n") + "\" | Out-File \"" + file + "\"";
        }

        @Override
        public String getFileTouchCommand(String file) {
            return "$error_count=$error.Count; Out-File -FilePath \"" + file + "\"; $LASTEXITCODE=$error.Count - $error_count";
        }

        @Override
        public String getOrConcatenationOperator() {
            return ";";
        }

        @Override
        public String getExecutable() {
            return "powershell";
        }

        @Override
        public String getPrintVariableCommand(String prefix, String name) {
            return "echo \"" + escapeStringValue(prefix) + "$" + escapeStringValue(name) + "\"";
        }

        @Override
        public String getPrintEnvironmentVariableCommand(String name) {
            return "echo \"" + "$env:" + escapeStringValue(name) + "\"";
        }

        @Override
        public String getSetEnvironmentVariableCommand(String variableName, String value) {
            return "$env:" + variableName + " = \"" + escapeStringValue(value) + "\"";
        }

        @Override
        public List<String> executeCommandListWithShell(String cmd) {
            return List.of("powershell", "-Command", cmd);
        }

        @Override
        public String getConcatenationOperator() {
            return ";";
        }

        @Override
        public String getMakeExecutableCommand(String file) {
            return "echo \"\"";
        }

        @Override
        public boolean doesRepeatInput() {
            return true;
        }

        @Override
        public String getScriptFileEnding() {
            return "ps1";
        }

        @Override
        public String getPauseCommand() {
            return "pause";
        }

        @Override
        public String prepareScriptContent(String content) {
            return content;
        }

        public String escapeStringValue(String input) {
            return input.replaceAll("[\"]", "`$0");
        }

        @Override
        public String getExitCodeVariable() {
            return "LASTEXITCODE";
        }

        @Override
        public String getEchoCommand(String s, boolean toErrorStream) {
            if (toErrorStream) {
                return String.format("$host.ui.WriteErrorLine('%s')", s);
            }

            return String.format("%s \"%s\"", "Write-Output", s);
        }

        @Override
        public String getNormalOpenCommand() {
            return "powershell /nologo";
        }

        @Override
        public String getInitFileOpenCommand(String file) {
            return "powershell.exe -NoExit -File \"" + file + "\"";
        }

        @Override
        public String executeCommandWithShell(String cmd) {
            return "powershell.exe -Command '" + cmd + "'";
        }

        @Override
        public String getFileReadCommand(String file) {
            return "cmd /c type \"" + file + "\"";
        }

        @Override
        public String getStreamFileWriteCommand(String file) {
            return "cmd /c 'findstr \"^\" > \"" + file + "\"'";
        }

        @Override
        public List<String> getMkdirsCommand(String dirs) {
            return List.of("cmd", "/c", "mkdir", dirs);
        }

        @Override
        public String getFileDeleteCommand(String file) {
            return "rm /path \"" + file + "\" -force";
        }

        @Override
        public String getFileExistsCommand(String file) {
            return String.format("cmd /c dir /a \"%s\"", file);
        }

        @Override
        public String getWhichCommand(String executable) {
            return "$LASTEXITCODE=(1 - (Get-Command -erroraction \"silentlycontinue\" \"" + executable + "\").Length)";
        }

        @Override
        public Charset determineCharset(ShellProcessControl control) throws Exception {
            var r = new BufferedReader(new InputStreamReader(control.getStdout(), StandardCharsets.US_ASCII));
            control.writeLine(
                    "If (Get-Command -erroraction 'silentlycontinue' chcp) {chcp} Else {echo \"Not Windows\"}");

            // Read echo of command
            r.readLine();
            // Read actual output
            var line = r.readLine();

            if (line.contains("Not Windows")) {
                return StandardCharsets.UTF_8;
            }

            var matcher = Pattern.compile("\\d+").matcher(line);
            matcher.find();
            return Charset.forName("ibm" + matcher.group());
        }

        @Override
        public NewLine getNewLine() {
            return NewLine.CRLF;
        }

        @Override
        public String getName() {
            return "powershell";
        }

        @Override
        public String getDisplayName() {
            return "PowerShell";
        }
    }

    public abstract static class PosixBase implements ShellType {

        @Override
        public String getInitFileOpenCommand(String file) {
            return getName() + " --rcfile \"" + file + "\"";
        }

        @Override
        public String getTextFileWriteCommand(String content, String file) {
            return "echo -e '" + content.replaceAll("\n", "\\\\n").replaceAll("'","\\\\'") + "' > \"" + file + "\"";
        }

        @Override
        public String getFileTouchCommand(String file) {
            return "touch \"" + file + "\"";
        }

        @Override
        public List<String> executeCommandListWithShell(String cmd) {
            return List.of(getExecutable(), "-c", cmd);
        }

        public String getScriptEchoCommand(String s) {
            return getEchoCommand(s, false) + "\nrm -- \"$0\"";
        }

        @Override
        public String getFileDeleteCommand(String file) {
            return "rm -rf \"" + file + "\"";
        }

        @Override
        public String getWhichCommand(String executable) {
            return "which \"" + executable + "\"";
        }

        @Override
        public String getScriptFileEnding() {
            return "sh";
        }

        @Override
        public String getMakeExecutableCommand(String file) {
            return "chmod +x \"" + file + "\"";
        }

        @Override
        public String addInlineVariablesToCommand(Map<String, String> variables, String command) {
            var content = "";
            for (Map.Entry<String, String> e : variables.entrySet()) {
                content += e.getKey() + "=\"" + e.getValue() + "\"";
                content += getConcatenationOperator();
            }
            return content + command;
        }

        @Override
        public String getPauseCommand() {
            return "bash -c read -rsp \"Press any key to continue...\" -n 1 key";
        }

        public abstract String getName();

        @Override
        public String prepareScriptContent(String content) {
            return content;
        }

        @Override
        public String getPrintVariableCommand(String prefix, String name) {
            return "echo " + prefix + "$" + name;
        }

        @Override
        public void disableHistory(ShellProcessControl pc) throws Exception {
            pc.executeLine("unset HISTFILE");
        }

        @Override
        public String getExitCommand() {
            return "exit 0";
        }

        @Override
        public String getConcatenationOperator() {
            return ";";
        }

        @Override
        public String getExitCodeVariable() {
            return "?";
        }

        @Override
        public String getEchoCommand(String s, boolean toErrorStream) {
            return "echo \"" + s + "\"" + (toErrorStream ? " 1>&2" : "");
        }

        @Override
        public String getSetEnvironmentVariableCommand(String variable, String value) {
            return "export " + variable + "=\"" + value + "\"";
        }

        @Override
        public String getNormalOpenCommand() {
            return getName();
        }

        @Override
        public String executeCommandWithShell(String cmd) {
            return getName() + " -c '" + cmd + "'";
        }

        @Override
        public List<String> getMkdirsCommand(String dirs) {
            return List.of("mkdir", "-p", dirs);
        }

        @Override
        public String getFileReadCommand(String file) {
            return "cat \"" + file + "\"";
        }

        @Override
        public String getStreamFileWriteCommand(String file) {
            return "cat > \"" + file + "\"";
        }

        @Override
        public String getFileExistsCommand(String file) {
            return String.format("test -f \"%s\" || test -d \"%s\"", file, file);
        }

        @Override
        public Charset determineCharset(ShellProcessControl st) throws Exception {
            return StandardCharsets.UTF_8;
        }

        @Override
        public NewLine getNewLine() {
            return NewLine.LF;
        }

        @Override
        public boolean doesRepeatInput() {
            return false;
        }
    }

    @JsonTypeName("sh")
    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Sh extends PosixBase {

        @Override
        public String getStreamFileWriteCommand(String file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getExecutable() {
            return "/bin/sh";
        }

        @Override
        public String getDisplayName() {
            return "/bin/sh";
        }

        @Override
        public String getName() {
            return "sh";
        }
    }

    @JsonTypeName("bash")
    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Bash extends PosixBase {

        @Override
        public String getExecutable() {
            return "/bin/bash";
        }

        @Override
        public String getDisplayName() {
            return "/bin/bash";
        }

        @Override
        public String getName() {
            return "bash";
        }
    }

    @JsonTypeName("zsh")
    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Zsh extends PosixBase {

        @Override
        public String getExecutable() {
            return "/bin/zsh";
        }

        @Override
        public String getDisplayName() {
            return "/bin/zsh";
        }

        @Override
        public String getName() {
            return "zsh";
        }
    }
}
