package io.xpipe.app.util;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import io.xpipe.app.issue.ErrorEvent;

import java.util.Optional;

public class WindowsRegistry {

    public static final int HKEY_CURRENT_USER = 0x80000001;
    public static final int HKEY_LOCAL_MACHINE = 0x80000002;

    public static Optional<String> readString(int hkey, String key) {
        return readString(hkey, key, null);
    }

    public static Optional<String> readString(int hkey, String key, String valueName) {
        // This can fail even with errors in case the jna native library extraction fails
        try {
            if (!Advapi32Util.registryValueExists(
                    hkey == HKEY_LOCAL_MACHINE ? WinReg.HKEY_LOCAL_MACHINE : WinReg.HKEY_CURRENT_USER, key, valueName)) {
                return Optional.empty();
            }

            return Optional.ofNullable(Advapi32Util.registryGetStringValue(
                    hkey == HKEY_LOCAL_MACHINE ? WinReg.HKEY_LOCAL_MACHINE : WinReg.HKEY_CURRENT_USER, key, valueName));
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).handle();
            return Optional.empty();
        }
    }
}