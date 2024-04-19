package io.xpipe.app.util;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;

import java.util.Optional;

public class WindowsRegistry {

    public static final int HKEY_CURRENT_USER = 0x80000001;
    public static final int HKEY_LOCAL_MACHINE = 0x80000002;

    public static boolean exists(int hkey, String key, String valueName) {
        // This can fail even with errors in case the jna native library extraction fails
        try {
            return Advapi32Util.registryValueExists(
                    hkey == HKEY_LOCAL_MACHINE ? WinReg.HKEY_LOCAL_MACHINE : WinReg.HKEY_CURRENT_USER, key, valueName);
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).handle();
            return false;
        }
    }

    public static Optional<String> readString(int hkey, String key) {
        return readString(hkey, key, null);
    }

    public static Optional<String> readString(int hkey, String key, String valueName) {
        // This can fail even with errors in case the jna native library extraction fails
        try {
            if (!Advapi32Util.registryValueExists(
                    hkey == HKEY_LOCAL_MACHINE ? WinReg.HKEY_LOCAL_MACHINE : WinReg.HKEY_CURRENT_USER,
                    key,
                    valueName)) {
                return Optional.empty();
            }

            return Optional.ofNullable(Advapi32Util.registryGetStringValue(
                    hkey == HKEY_LOCAL_MACHINE ? WinReg.HKEY_LOCAL_MACHINE : WinReg.HKEY_CURRENT_USER, key, valueName));
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).handle();
            return Optional.empty();
        }
    }

    public static boolean remoteKeyExists(ShellControl shellControl, int hkey, String key) throws Exception {
        var command = CommandBuilder.of()
                .add("reg", "query")
                .addQuoted((hkey == HKEY_LOCAL_MACHINE ? "HKEY_LOCAL_MACHINE" : "HKEY_CURRENT_USER") + "\\" + key)
                .add("/ve");
        try (var c = shellControl.command(command).start()) {
            return c.discardAndCheckExit();
        }
    }

    public static Optional<String> findRemoteValuesRecursive(ShellControl shellControl, int hkey, String key, String valueName) throws Exception {
        var command = CommandBuilder.of()
                .add("reg", "query")
                .addQuoted((hkey == HKEY_LOCAL_MACHINE ? "HKEY_LOCAL_MACHINE" : "HKEY_CURRENT_USER") + "\\" + key)
                .add("/v")
                .addQuoted(valueName)
                .add("/s");
        try (var c = shellControl.command(command).start()) {
            var output = c.readStdoutDiscardErr();
            if (c.getExitCode() != 0) {
                return Optional.empty();
            } else {
                return Optional.of(output);
            }
        }
    }

    public static Optional<String> readRemoteString(ShellControl shellControl, int hkey, String key, String valueName)
            throws Exception {
        var command = CommandBuilder.of()
                .add("reg", "query")
                .addQuoted((hkey == HKEY_LOCAL_MACHINE ? "HKEY_LOCAL_MACHINE" : "HKEY_CURRENT_USER") + "\\" + key)
                .add("/v")
                .addQuoted(valueName);

        String output;
        try (var c = shellControl.command(command).start()) {
            output = c.readStdoutDiscardErr();
            if (c.getExitCode() != 0) {
                return Optional.empty();
            }
        }

        // Output has the following format:
        // \n<Version information>\n\n<key>\t<registry type>\t<value>
        if (output.contains("\t")) {
            String[] parsed = output.split("\t");
            return Optional.of(parsed[parsed.length - 1]);
        }

        if (output.contains("    ")) {
            String[] parsed = output.split("    ");
            return Optional.of(parsed[parsed.length - 1]);
        }

        return Optional.empty();
    }
}
