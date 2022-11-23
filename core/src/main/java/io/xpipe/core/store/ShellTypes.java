package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.charsetter.NewLine;
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

    public static ShellType getRecommendedDefault() {
        if (System.getProperty("os.name").startsWith("Windows")) {
            return POWERSHELL;
        } else {
            return SH;
        }
    }

    public static ShellType getPlatformDefault() {
        if (System.getProperty("os.name").startsWith("Windows")) {
            return CMD;
        } else {
            return SH;
        }
    }

    public static ShellType[] getWindowsShells() {
        return new ShellType[] {CMD, POWERSHELL};
    }

    public static ShellType[] getLinuxShells() {
        return new ShellType[] {SH};
    }

    @JsonTypeName("cmd")
    @Value
    public static class Cmd implements ShellType {

        @Override
        public String getSetVariableCommand(String variableName, String value) {
            return "set \"" + variableName + "=" + value + "\"";
        }

        @Override
        public String getEchoCommand(String s, boolean toErrorStream) {
            return toErrorStream ? "(echo " + s + ")1>&2" : "echo " + s;
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
            return "!errorlevel!";
        }

        @Override
        public NewLine getNewLine() {
            return NewLine.CRLF;
        }

        @Override
        public List<String> openCommand() {
            return List.of("cmd", "/V:on");
        }

        @Override
        public String switchTo(String cmd) {
            return "cmd.exe /V:on /c " + cmd;
        }

        @Override
        public List<String> createMkdirsCommand(String dirs) {
            return List.of("mkdir", dirs);
        }

        @Override
        public List<String> createFileReadCommand(String file) {
            return List.of("type", file);
        }

        @Override
        public List<String> createFileWriteCommand(String file) {
            return List.of("findstr", "\"^\"", ">", file);
        }

        @Override
        public List<String> createFileExistsCommand(String file) {
            return List.of("dir", "/a", file);
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
            return "cmd";
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
        public String getSetVariableCommand(String variableName, String value) {
            return "set " + variableName + "=" + value;
        }

        @Override
        public String queryShellProcessId(ShellProcessControl control) throws IOException {
            control.writeLine("powershell (Get-WmiObject Win32_Process -Filter ProcessId=$PID).ParentProcessId");

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
        public boolean echoesInput() {
            return true;
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
            return "$LASTEXITCODE";
        }

        @Override
        public String getEchoCommand(String s, boolean toErrorStream) {
            if (toErrorStream) {
                return String.format("$host.ui.WriteErrorLine('%s')", s);
            }

            return String.format("%s \"%s\"", "Write-Output", s);
        }

        @Override
        public List<String> openCommand() {
            return List.of("powershell", "/nologo");
        }

        @Override
        public String switchTo(String cmd) {
            return "powershell.exe -Command " + cmd;
        }

        @Override
        public List<String> createFileReadCommand(String file) {
            return List.of("cmd", "/c", "type", file);
        }

        @Override
        public List<String> createFileWriteCommand(String file) {
            return List.of("cmd", "/c", "findstr", "\"^\"", ">", file);
        }

        @Override
        public List<String> createMkdirsCommand(String dirs) {
            return List.of("cmd", "/c", "mkdir", dirs);
        }

        @Override
        public List<String> createFileExistsCommand(String file) {
            return List.of("cmd", "/c", "dir", "/a", file);
        }

        @Override
        public Charset determineCharset(ShellProcessControl control) throws Exception {
            control.writeLine("chcp");

            var r = new BufferedReader(new InputStreamReader(control.getStdout(), StandardCharsets.US_ASCII));
            // Read echo of command
            r.readLine();
            // Read actual output
            var line = r.readLine();

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

    @JsonTypeName("sh")
    @Value
    public static class Sh implements ShellType {

        @Override
        public String getExitCommand() {
            return "exit 0";
        }

        @Override
        public String getConcatenationOperator() {
            return ";";
        }

        @Override
        public void elevate(ShellProcessControl control, String command, String displayCommand) throws Exception {
            if (control.getElevationPassword() == null) {
                control.executeCommand("SUDO_ASKPASS=/bin/false sudo -p \"\" -S  " + command);
                return;
            }

            // For sudo to always query for a password by using the -k switch
            control.executeCommand("sudo -p \"\" -k -S " + command);
            control.writeLine(control.getElevationPassword().getSecretValue());
        }

        @Override
        public String getExitCodeVariable() {
            return "$?";
        }

        @Override
        public String getEchoCommand(String s, boolean toErrorStream) {
            return "echo " + s + (toErrorStream ? " 1>&2" : "");
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
            return variableName + "=" + value;
        }

        @Override
        public List<String> openCommand() {
            return List.of("sh", "-i", "-l");
        }

        @Override
        public String switchTo(String cmd) {
            return "sh -c \"" + cmd + "\"";
        }

        @Override
        public List<String> createMkdirsCommand(String dirs) {
            return List.of("mkdir", "-p", dirs);
        }

        @Override
        public List<String> createFileReadCommand(String file) {
            return List.of("cat", file);
        }

        @Override
        public List<String> createFileWriteCommand(String file) {
            return List.of("cat", ">", file);
        }

        @Override
        public List<String> createFileExistsCommand(String file) {
            return List.of("(", "test", "-f", file, "||", "test", "-d", file, ")");
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
        public String getName() {
            return "sh";
        }

        @Override
        public String getDisplayName() {
            return "/bin/sh";
        }

        @Override
        public boolean echoesInput() {
            return false;
        }
    }
}
