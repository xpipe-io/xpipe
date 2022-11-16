package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.charsetter.NewLine;
import lombok.Value;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

public class ShellTypes {

    public static final ShellType POWERSHELL = new PowerShell();
    public static final ShellType CMD = new Cmd();
    public static final ShellType SH = new Sh();

    public static ShellType determine(ProcessControl proc) throws Exception {
        proc.writeLine("echo -NoEnumerate \"a\"", false);
        String line;
        while (true) {
            line = proc.readLine();
            if (line.equals("-NoEnumerate a")) {
                return SH;
            }

            if (line.contains("echo -NoEnumerate \"a\"")) {
                break;
            }
        }

        var o = proc.readLine();

        if (o.equals("a")) {
            return POWERSHELL;
        } else if (o.equals("-NoEnumerate \"a\"")) {
            return CMD;
        } else {
            return SH;
        }
    }

    public static ShellType[] getAvailable(ShellStore store) throws Exception {
        try (ProcessControl proc = store.create().start()) {
            var type = determine(proc);
            if (type == SH) {
                return getLinuxShells();
            } else {
                return getWindowsShells();
            }
        }
    }

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
        public String getEchoCommand(String s, boolean newLine) {
            return newLine ? "echo " + s : "echo | set /p dummyName=" + s;
        }

        @Override
        public String getConcatenationOperator() {
            return "&";
        }

        @Override
        public void elevate(ShellProcessControl control, String command, String displayCommand) throws IOException {
            control.executeCommand("net session >NUL 2>NUL");
            control.executeCommand("echo %errorLevel%");
            var exitCode = Integer.parseInt(control.readLine());
            if (exitCode != 0) {
                throw new IllegalStateException("The command \"" + displayCommand + "\" requires elevation.");
            }

            control.executeCommand(command);
        }

        @Override
        public void init(ProcessControl proc) throws IOException {
            proc.readLine();
            proc.readLine();
            proc.readLine();
        }

        @Override
        public String getExitCodeVariable() {
            return "%errorlevel%";
        }

        @Override
        public NewLine getNewLine() {
            return NewLine.CRLF;
        }

        @Override
        public List<String> openCommand() {
            return List.of("cmd");
        }

        @Override
        public String switchTo(String cmd) {
            return "cmd.exe /c " + cmd;
        }

        @Override
        public List<String> createMkdirsCommand(String dirs) {
            return List.of("lmkdir", dirs);
        }

        @Override
        public List<String> createFileReadCommand(String file) {
            return List.of("type", file);
        }

        @Override
        public List<String> createFileWriteCommand(String file) {
            return List.of("Out-File", "-FilePath", file);
        }

        @Override
        public List<String> createFileExistsCommand(String file) {
            return List.of("if", "exist", file, "echo", "hi");
        }

        @Override
        public Charset determineCharset(ProcessControl control) throws Exception {
            var output = control.readResultLine("chcp", true);
            var matcher = Pattern.compile("\\d+").matcher(output);
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
        public List<String> getOperatingSystemNameCommand() {
            return List.of("Get-ComputerInfo");
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
        public boolean echoesInput() {
            return true;
        }

        @Override
        public void elevate(ShellProcessControl control, String command, String displayCommand) throws IOException {
            control.executeCommand("([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltinRole]::Administrator)");
            var exitCode = Integer.parseInt(control.readLine());
            if (exitCode != 0) {
                throw new IllegalStateException("The command \"" + displayCommand + "\" requires elevation.");
            }

            control.executeCommand(command);
        }

        @Override
        public String getExitCodeVariable() {
            return "$LASTEXITCODE";
        }

        @Override
        public String getEchoCommand(String s, boolean newLine) {
            return newLine ? "echo " + s : String.format("Write-Host \"%s\" -NoNewLine", s);
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
        public List<String> createMkdirsCommand(String dirs) {
            return List.of("New-Item", "-Path", "D:\\temp\\Test Folder", "-ItemType", "Directory");
        }

        @Override
        public List<String> createFileReadCommand(String file) {
            return List.of("Get-Content", file);
        }

        @Override
        public List<String> createFileWriteCommand(String file) {
            return List.of("Out-File", "-FilePath", file);
        }

        @Override
        public List<String> createFileExistsCommand(String file) {
            return List.of("Test-Path", "-path", file);
        }

        @Override
        public Charset determineCharset(ProcessControl control) throws Exception {
            var output = control.readResultLine("chcp", true);
            var matcher = Pattern.compile("\\d+").matcher(output);
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

        @Override
        public List<String> getOperatingSystemNameCommand() {
            return List.of("systeminfo", "|", "findstr", "/B", "/C:\"OS Name\"");
        }
    }

    @JsonTypeName("sh")
    @Value
    public static class Sh implements ShellType {

        @Override
        public void elevate(ShellProcessControl control, String command, String displayCommand) throws IOException {
            if (control.getElevationPassword().getSecretValue() == null) {
                throw new IllegalStateException("No password for sudo has been set");
            }

            control.executeCommand("sudo -S " + switchTo(command));
            control.writeLine(control.getElevationPassword().getSecretValue());
        }

        @Override
        public String getExitCodeVariable() {
            return "$?";
        }

        @Override
        public String getEchoCommand(String s, boolean newLine) {
            return newLine ? "echo " + s : "echo -n " + s;
        }

        @Override
        public List<String> openCommand() {
            return List.of("sh");
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
            return List.of(file);
        }

        @Override
        public List<String> createFileExistsCommand(String file) {
            return List.of("test", "-f", file, "||", "test", "-d", file);
        }

        @Override
        public Charset determineCharset(ProcessControl st) throws Exception {
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
        public List<String> getOperatingSystemNameCommand() {
            return List.of("uname", "-o");
        }

        @Override
        public boolean echoesInput() {
            return false;
        }
    }
}
