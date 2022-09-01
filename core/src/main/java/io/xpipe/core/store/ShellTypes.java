package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.util.Secret;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ShellTypes {

    public static StandardShellStore.ShellType determine(ShellStore store) throws Exception {
        var o = store.executeAndCheckOut(List.of(), List.of("echo", "$0"), null).strip();
        if (!o.equals("$0")) {
            return SH;
        } else {
            o = store.executeAndCheckOut(List.of(), List.of("(dir 2>&1 *`|echo CMD);&<# rem #>echo PowerShell"), null).trim();
            if (o.equals("PowerShell")) {
                return POWERSHELL;
            } else
                return CMD;
            }
        }

    public static StandardShellStore.ShellType[] getAvailable(ShellStore store) throws Exception {
        var o = store.executeAndCheckOut(List.of(), List.of("echo", "$0"), null);
        if (!o.trim().equals("$0")) {
            return getLinuxShells();
        } else {
            return getWindowsShells();
        }
    }

    @JsonProperty("powershell")
    public static final StandardShellStore.ShellType POWERSHELL = new StandardShellStore.ShellType() {

        @Override
        public List<String> switchTo(List<String> cmd) {
            var l = new ArrayList<>(cmd);
            l.add(0, "powershell.exe");
            return l;
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
        public Charset getCharset() {
            return StandardCharsets.UTF_16LE;
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
    };

    @JsonProperty("cmd")
    public static final StandardShellStore.ShellType CMD = new StandardShellStore.ShellType() {

        @Override
        public NewLine getNewLine() {
            return NewLine.CRLF;
        }

        @Override
        public List<String> switchTo(List<String> cmd) {
            var l = new ArrayList<>(cmd);
            l.add(0, "cmd.exe");
            l.add(1, "/c");
            return l;
        }

        @Override
        public ProcessControl prepareElevatedCommand(ShellStore st, List<Secret> in, List<String> cmd, Integer timeout, String pw) throws Exception {
            var l = List.of("net", "session", ";", "if", "%errorLevel%", "!=", "0");
            return st.prepareCommand(List.of(), l, timeout);
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
        public Charset getCharset() {
            return StandardCharsets.UTF_16LE;
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
        public List<String> getOperatingSystemNameCommand() {
            return List.of("Get-ComputerInfo");
        }
    };

    @JsonProperty("sh")
    public static final StandardShellStore.ShellType SH = new StandardShellStore.ShellType() {

        @Override
        public List<String> switchTo(List<String> cmd) {
            return cmd;
        }

        @Override
        public ProcessControl prepareElevatedCommand(ShellStore st, List<Secret> in, List<String> cmd, Integer timeout, String pw) throws Exception {
            var l = new ArrayList<>(cmd);
            l.add(0, "sudo");
            l.add(1, "-S");
            var pws = new ByteArrayInputStream(pw.getBytes(getCharset()));
            return st.prepareCommand(List.of(Secret.createForSecretValue(pw)), l, timeout);
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
        public Charset getCharset() {
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
    };

    public static StandardShellStore.ShellType getDefault() {
        if (System.getProperty("os.name").startsWith("Windows")) {
            return CMD;
        } else {
            return SH;
        }
    }


    public static StandardShellStore.ShellType[] getWindowsShells() {
        return new StandardShellStore.ShellType[]{CMD, POWERSHELL};
    }

    public static StandardShellStore.ShellType[] getLinuxShells() {
        return new StandardShellStore.ShellType[]{SH};
    }

    private final String name;

    ShellTypes(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
