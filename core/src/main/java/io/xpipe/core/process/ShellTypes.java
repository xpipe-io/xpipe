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
import java.util.List;
import java.util.regex.Pattern;

public class ShellTypes {

    public static final ShellType POWERSHELL = new PowerShell();
    public static final ShellType CMD = new Cmd();
    public static final ShellType SH = new Sh();
    public static final ShellType BASH = new Bash();

    public static ShellType getPlatformDefault() {
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            return CMD;
        } else {
            return BASH;
        }
    }

    public static ShellType[] getAllShellTypes() {
        return new ShellType[] {CMD, POWERSHELL, BASH, SH};
    }

    @JsonTypeName("cmd")
    @Value
    public static class Cmd implements ShellType {

        @Override
        public String getSetVariableCommand(String variableName, String value) {
            return ("set \"" + variableName + "=" + value.replaceAll("\"", "^$0") + "\"");
        }

        @Override
        public String getPrintVariableCommand(String prefix, String name) {
            return "call echo " + prefix + "^%" + name + "^%";
        }

        @Override
        public String getEchoCommand(String s, boolean toErrorStream) {
            return "(echo " + s + (toErrorStream ? ")1>&2" : ")");
        }

        @Override
        public String getScriptEchoCommand(String s) {
            return ("@echo off\r\nset \"echov=" + escapeStringValue(s)
                    + "\"\r\necho %echov%\r\n@echo on\n(goto) 2>nul & del \"%~f0\"");
        }

        @Override
        public String queryShellProcessId(ShellProcessControl control) throws IOException {
            control.writeLine("powershell (Get-WmiObject Win32_Process -Filter ProcessId=$PID).ParentProcessId");

            var r = new BufferedReader(new InputStreamReader(control.getStdout(), StandardCharsets.US_ASCII));
            // Read echo of command
            r.readLine();
            // Read actual output
            var line = r.readLine();
            r.readLine();
            return line;
        }

        @Override
        public String getConcatenationOperator() {
            return "&";
        }

        @Override
        public String getMakeExecutableCommand(String file) {
            return "echo.";
        }

        @Override
        public String getTerminalFileOpenCommand(String file) {
            return String.format("%s %s \"%s\"", getExecutable(), "/C", file);
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
        public String createInitFileContent(String command) {
            return "@echo off\n" + command;
        }

        @Override
        public void elevate(ShellProcessControl control, String command, String displayCommand) throws Exception {
            try (CommandProcessControl c = control.command("net session >NUL 2>NUL")) {
                var exitCode = c.getExitCode();
                if (exitCode != 0) {
                    throw new IllegalStateException("The command \"" + displayCommand + "\" requires elevation.");
                }
            }

            control.executeCommand(command);
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
        public String executeCommandWithShell(String cmd) {
            return "cmd.exe /C " + cmd + "";
        }

        @Override
        public List<String> executeCommandListWithShell(String cmd) {
            return List.of("cmd", "/C", cmd);
        }

        @Override
        public List<String> createMkdirsCommand(String dirs) {
            return List.of("(", "if", "not", "exist", dirs, "mkdir", dirs, ")");
        }

        @Override
        public String createFileReadCommand(String file) {
            return "type \"" + file + "\"";
        }

        @Override
        public String createFileWriteCommand(String file) {
            return "findstr \"^\" > \"" + file + "\"";
        }

        @Override
        public String createFileDeleteCommand(String file) {
            return "rd /s /q \"" + file + "\"";
        }

        @Override
        public String createFileExistsCommand(String file) {
            return String.format("dir /a \"%s\"", file);
        }

        @Override
        public String createWhichCommand(String executable) {
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
        public boolean echoesInput() {
            return true;
        }
    }

    @JsonTypeName("powershell")
    @Value
    public static class PowerShell implements ShellType {

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
        public String getSetVariableCommand(String variableName, String value) {
            return "$env:" + variableName + " = \"" + escapeStringValue(value) + "\"";
        }

        @Override
        public List<String> executeCommandListWithShell(String cmd) {
            return List.of("powershell", "-Command", cmd);
        }

        @Override
        public String queryShellProcessId(ShellProcessControl control) throws IOException {
            control.writeLine("echo $PID");

            var r = new BufferedReader(new InputStreamReader(control.getStdout(), StandardCharsets.US_ASCII));
            // Read echo of command
            r.readLine();
            // Read actual output
            var line = r.readLine();
            return line;
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
        public boolean echoesInput() {
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
        public String createInitFileContent(String command) {
            return command;
        }

        @Override
        public String getTerminalFileOpenCommand(String file) {
            return String.format("%s -ExecutionPolicy Bypass -File \"%s\"", getExecutable(), file);
        }

        public String escapeStringValue(String input) {
            return input.replaceAll("[\"]", "`$0");
        }

        @Override
        public void elevate(ShellProcessControl control, String command, String displayCommand) throws Exception {
            try (CommandProcessControl c = control.command(
                            "([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltinRole]::Administrator)")
                    .start()) {
                if (c.startAndCheckExit()) {
                    throw new IllegalStateException("The command \"" + displayCommand + "\" requires elevation.");
                }
            }

            control.executeCommand(command);
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
        public String executeCommandWithShell(String cmd) {
            return "powershell.exe -Command '" + cmd + "'";
        }

        @Override
        public String createFileReadCommand(String file) {
            return "cmd /c type \""+ file + "\"";
        }

        @Override
        public String createFileWriteCommand(String file) {
            return "cmd /c 'findstr \"^\" > \"" + file + "\"'";
        }

        @Override
        public List<String> createMkdirsCommand(String dirs) {
            return List.of("cmd", "/c", "mkdir", dirs);
        }

        @Override
        public String createFileDeleteCommand(String file) {
            return "rm /path \"" + file + "\" -force";
        }

        @Override
        public String createFileExistsCommand(String file) {
            return String.format("cmd /c dir /a \"%s\"", file);
        }

        @Override
        public String createWhichCommand(String executable) {
            return "$LASTEXITCODE=(1 - (Get-Command -erroraction \"silentlycontinue\" \"" + executable + "\").Length)";
        }

        @Override
        public Charset determineCharset(ShellProcessControl control) throws Exception {
            var r = new BufferedReader(new InputStreamReader(control.getStdout(), StandardCharsets.US_ASCII));
            control.writeLine("If (Get-Command -erroraction 'silentlycontinue' chcp) {chcp} Else {echo \"Not Windows\"}");

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
        public List<String> executeCommandListWithShell(String cmd) {
            return List.of(getExecutable(), "-c", cmd);
        }

        public String getScriptEchoCommand(String s) {
            return "#!" + getExecutable() + "\n" + getEchoCommand(s, false) + "\nrm -- \"$0\"";
        }

        @Override
        public String createFileDeleteCommand(String file) {
            return "rm -rf \"" + file + "\"";
        }

        @Override
        public String createWhichCommand(String executable) {
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
        public String commandWithVariable(String key, String value, String command) {
            return getSetVariableCommand(key, value) + " " + command;
        }

        @Override
        public String getPauseCommand() {
            return "bash -c read -rsp \"Press any key to continue...\n\" -n 1 key";
        }

        public abstract String getName();

        @Override
        public String createInitFileContent(String command) {
            return command;
        }

        @Override
        public String getPrintVariableCommand(String prefix, String name) {
            return "echo " + prefix + "$" + name;
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
        public String getTerminalFileOpenCommand(String file) {
            return String.format("%s -i -c \"%s\"", getExecutable(), file);
        }

        @Override
        public void elevate(ShellProcessControl control, String command, String displayCommand) throws Exception {
            if (control.getElevationPassword() == null) {
                control.executeCommand("SUDO_ASKPASS=/bin/false sudo -n -p \"\" -S -- " + command);
                return;
            }

            // For sudo to always query for a password by using the -k switch
            control.executeCommand("sudo -p \"\" -k -S -- " + command);
            control.writeLine(control.getElevationPassword().getSecretValue());
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
        public String queryShellProcessId(ShellProcessControl control) throws Exception {
            try (CommandProcessControl c = control.command("echo $$").start()) {
                var out = c.readOnlyStdout();
                var matcher = Pattern.compile("\\d+$").matcher(out);
                matcher.find();
                return matcher.group(0);
            }
        }

        @Override
        public String getSetVariableCommand(String variableName, String value) {
            return variableName + "=\"" + value + "\"";
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
        public List<String> createMkdirsCommand(String dirs) {
            return List.of("mkdir", "-p", dirs);
        }

        @Override
        public String createFileReadCommand(String file) {
            return "cat \"" + file + "\"";
        }

        @Override
        public String createFileWriteCommand(String file) {
            return "cat > \"" + file + "\"";
        }

        @Override
        public String createFileExistsCommand(String file) {
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
        public boolean echoesInput() {
            return false;
        }
    }

    @JsonTypeName("sh")
    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Sh extends PosixBase {

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
}
