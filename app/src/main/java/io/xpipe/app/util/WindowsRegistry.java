package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.OsType;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.win32.W32APIOptions;
import lombok.Value;

import java.util.*;

public abstract class WindowsRegistry {

    public static final int HKEY_CURRENT_USER = 0x80000001;
    public static final int HKEY_LOCAL_MACHINE = 0x80000002;

    public static void init() {
        if (OsType.ofLocal() != OsType.WINDOWS) {
            return;
        }

        // Load lib
        Local.isLibrarySupported();
    }

    public static WindowsRegistry.Local local() {
        return new Local();
    }

    public static WindowsRegistry ofShell(ShellControl shellControl) {
        return shellControl.isLocal() ? new Local() : new Remote(shellControl);
    }

    public abstract boolean keyExists(int hkey, String key) throws Exception;

    public abstract List<String> listSubKeys(int hkey, String key) throws Exception;

    public abstract boolean valueExists(int hkey, String key, String valueName) throws Exception;

    public abstract OptionalInt readIntegerValueIfPresent(int hkey, String key, String valueName) throws Exception;

    public abstract Optional<String> readStringValueIfPresent(int hkey, String key, String valueName) throws Exception;

    public Optional<String> readStringValueIfPresent(int hkey, String key) throws Exception {
        return readStringValueIfPresent(hkey, key, null);
    }

    public abstract Optional<String> findValuesRecursive(int hkey, String key, String valueName) throws Exception;

    public abstract Optional<Key> findKeyForEqualValueMatchRecursive(int hkey, String key, String match)
            throws Exception;

    @Value
    public static class Key {
        int hkey;
        String key;
    }

    public static class Local extends WindowsRegistry {

        private WinReg.HKEY hkey(int hkey) {
            return hkey == HKEY_LOCAL_MACHINE ? WinReg.HKEY_LOCAL_MACHINE : WinReg.HKEY_CURRENT_USER;
        }

        private static Boolean libraryLoaded;

        private static synchronized boolean isLibrarySupported() {
            if (libraryLoaded != null) {
                return libraryLoaded;
            }

            try {
                Native.load("Advapi32", Advapi32.class, W32APIOptions.DEFAULT_OPTIONS);
                return (libraryLoaded = true);
            } catch (Throwable t) {
                libraryLoaded = false;
                ErrorEventFactory.fromThrowable(t)
                        .description("Unable to load native library Advapi32.dll for registry queries."
                                + " Registry queries will fail and some functionality will be unavailable")
                        .handle();
                return false;
            }
        }

        @Override
        public boolean keyExists(int hkey, String key) {
            if (!isLibrarySupported()) {
                return false;
            }

            try {
                return Advapi32Util.registryKeyExists(hkey(hkey), key);
            } catch (Win32Exception ignored) {
                return false;
            }
        }

        @Override
        public List<String> listSubKeys(int hkey, String key) {
            if (!isLibrarySupported()) {
                return List.of();
            }

            try {
                return Arrays.asList(Advapi32Util.registryGetKeys(hkey(hkey), key));
            } catch (Win32Exception ignored) {
                return List.of();
            }
        }

        @Override
        public boolean valueExists(int hkey, String key, String valueName) {
            if (!isLibrarySupported()) {
                return false;
            }

            try {
                return Advapi32Util.registryValueExists(hkey(hkey), key, valueName);
            } catch (Win32Exception ignored) {
                return false;
            }
        }

        @Override
        public OptionalInt readIntegerValueIfPresent(int hkey, String key, String valueName) {
            if (!isLibrarySupported()) {
                return OptionalInt.empty();
            }

            try {
                if (!Advapi32Util.registryValueExists(hkey(hkey), key, valueName)) {
                    return OptionalInt.empty();
                }

                return OptionalInt.of(Advapi32Util.registryGetIntValue(hkey(hkey), key, valueName));
            } catch (Win32Exception ignored) {
                return OptionalInt.empty();
            }
        }

        @Override
        public Optional<String> readStringValueIfPresent(int hkey, String key, String valueName) {
            if (!isLibrarySupported()) {
                return Optional.empty();
            }

            try {
                if (!Advapi32Util.registryValueExists(hkey(hkey), key, valueName)) {
                    return Optional.empty();
                }

                return Optional.ofNullable(Advapi32Util.registryGetStringValue(hkey(hkey), key, valueName));
            } catch (Win32Exception ignored) {
                return Optional.empty();
            }
        }

        @Override
        public Optional<String> findValuesRecursive(int hkey, String key, String valueName) throws Exception {
            if (!isLibrarySupported()) {
                return Optional.empty();
            }

            try (var sc = LocalShell.getShell().start()) {
                return new Remote(sc).findValuesRecursive(hkey, key, valueName);
            }
        }

        @Override
        public Optional<Key> findKeyForEqualValueMatchRecursive(int hkey, String key, String match) throws Exception {
            if (!isLibrarySupported()) {
                return Optional.empty();
            }

            try (var sc = LocalShell.getShell().start()) {
                return new Remote(sc).findKeyForEqualValueMatchRecursive(hkey, key, match);
            }
        }
    }

    public static class Remote extends WindowsRegistry {

        private final ShellControl shellControl;

        public Remote(ShellControl shellControl) {
            this.shellControl = shellControl;
        }

        public static Optional<String> readOutputValue(String original) {
            // Output has the following format:
            // \n<Version information>\n\n<key>\t<registry type>\t<value>
            if (original.contains("\t")) {
                String[] parsed = original.split("\t");
                if (parsed.length < 4) {
                    return Optional.empty();
                }
                return Optional.of(parsed[parsed.length - 1]);
            }

            if (original.contains("    ")) {
                String[] parsed = original.split(" {4}");
                if (parsed.length < 4) {
                    return Optional.empty();
                }
                return Optional.of(parsed[parsed.length - 1]);
            }

            return Optional.empty();
        }

        private String hkey(int hkey) {
            return hkey == HKEY_LOCAL_MACHINE ? "HKEY_LOCAL_MACHINE" : "HKEY_CURRENT_USER";
        }

        @Override
        public boolean keyExists(int hkey, String key) throws Exception {
            var command = CommandBuilder.of()
                    .add("reg", "query")
                    .addQuoted(hkey(hkey) + "\\" + key)
                    .add("/ve");
            try (var c = shellControl.command(command).start()) {
                return c.discardAndCheckExit();
            }
        }

        @Override
        public List<String> listSubKeys(int hkey, String key) throws Exception {
            var prefix = hkey(hkey) + "\\" + key;
            var command = CommandBuilder.of().add("reg", "query").addQuoted(prefix);
            var out = shellControl.command(command).readStdoutOrThrow();
            return out.lines()
                    .filter(s -> {
                        return s.contains(prefix + "\\");
                    })
                    .map(s -> s.replace(prefix + "\\", ""))
                    .toList();
        }

        @Override
        public boolean valueExists(int hkey, String key, String valueName) throws Exception {
            var command = CommandBuilder.of()
                    .add("reg", "query")
                    .addQuoted(hkey(hkey) + "\\" + key)
                    .add("/v")
                    .addQuoted(valueName);
            try (var c = shellControl.command(command).start()) {
                return c.discardAndCheckExit();
            }
        }

        @Override
        public OptionalInt readIntegerValueIfPresent(int hkey, String key, String valueName) throws Exception {
            var r = readStringValueIfPresent(hkey, key, valueName);
            if (r.isPresent()) {
                return OptionalInt.of(Integer.decode(r.get()));
            } else {
                return OptionalInt.empty();
            }
        }

        @Override
        public Optional<String> readStringValueIfPresent(int hkey, String key, String valueName) throws Exception {
            var command = CommandBuilder.of()
                    .add("reg", "query")
                    .addQuoted(hkey(hkey) + "\\" + key)
                    .add("/v")
                    .addQuoted(valueName);

            var output = shellControl.command(command).readStdoutIfPossible();
            if (output.isEmpty()) {
                return Optional.empty();
            }

            return readOutputValue(output.get());
        }

        @Override
        public Optional<String> findValuesRecursive(int hkey, String key, String valueName) throws Exception {
            var command = CommandBuilder.of()
                    .add("reg", "query")
                    .addQuoted(hkey(hkey) + "\\" + key)
                    .add("/v")
                    .addQuoted(valueName)
                    .add("/s");
            return shellControl.command(command).readStdoutIfPossible();
        }

        @Override
        public Optional<Key> findKeyForEqualValueMatchRecursive(int hkey, String key, String match) throws Exception {
            var command = CommandBuilder.of()
                    .add("reg", "query")
                    .addQuoted(hkey(hkey) + "\\" + key)
                    .add("/f")
                    .addQuoted(match)
                    .add("/s")
                    .add("/e")
                    .add("/d");
            return shellControl.command(command).readStdoutIfPossible().flatMap(output -> {
                return output.lines().findFirst().flatMap(s -> {
                    if (s.startsWith("HKEY_CURRENT_USER\\")) {
                        return Optional.of(new Key(HKEY_CURRENT_USER, s.replace("HKEY_CURRENT_USER\\", "")));
                    }
                    if (s.startsWith("HKEY_LOCAL_MACHINE\\")) {
                        return Optional.of(new Key(HKEY_LOCAL_MACHINE, s.replace("HKEY_LOCAL_MACHINE\\", "")));
                    }
                    return Optional.empty();
                });
            });
        }
    }
}
