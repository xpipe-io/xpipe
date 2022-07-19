package io.xpipe.core.store;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ShellTypes {

    public static StandardShellStore.ShellType determine(ShellStore store) throws Exception {
        var o = store.executeAndCheckOut(InputStream.nullInputStream(), List.of("echo", "$0"));
        if (o.isPresent() && !o.get().equals("$0")) {
            return SH;
        } else {
            o = store.executeAndCheckOut(InputStream.nullInputStream(), List.of("(dir 2>&1 *`|echo CMD);&<# rem #>echo PowerShell"));
            if (o.isPresent() && o.get().equals("PowerShell")) {
                return POWERSHELL;
            } else {
                return CMD;
            }
        }
    }

    public static StandardShellStore.ShellType[] getAvailable(ShellStore store) throws Exception {
        var o = store.executeAndCheckOut(InputStream.nullInputStream(), List.of("echo", "$0"));
        if (o.isPresent() && !o.get().trim().equals("$0")) {
            return getLinuxShells();
        } else {
            return getWindowsShells();
        }
    }

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
        public String getName() {
            return "powershell";
        }

        @Override
        public String getDisplayName() {
            return "PowerShell";
        }
    };

    public static final StandardShellStore.ShellType CMD = new StandardShellStore.ShellType() {

        @Override
        public List<String> switchTo(List<String> cmd) {
            var l = new ArrayList<>(cmd);
            l.add(0, "cmd.exe");
            l.add(1, "/c");
            return l;
        }

        @Override
        public ProcessControl prepareElevatedCommand(ShellStore st, InputStream in, List<String> cmd, String pw) throws Exception {
            var l = List.of("net", "session", ";", "if", "%errorLevel%", "!=", "0");
            return st.prepareCommand(InputStream.nullInputStream(), l);
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
    };

    public static final StandardShellStore.ShellType SH = new StandardShellStore.ShellType() {

        @Override
        public List<String> switchTo(List<String> cmd) {
            return cmd;
        }

        @Override
        public ProcessControl prepareElevatedCommand(ShellStore st, InputStream in, List<String> cmd, String pw) throws Exception {
            var l = new ArrayList<>(cmd);
            l.add(0, "sudo");
            l.add(1, "-S");
            var pws = new ByteArrayInputStream(pw.getBytes(getCharset()));
            return st.prepareCommand(pws, l);
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
        public String getName() {
            return "sh";
        }

        @Override
        public String getDisplayName() {
            return "/bin/sh";
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
