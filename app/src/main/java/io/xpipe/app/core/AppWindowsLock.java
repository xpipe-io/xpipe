package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ThreadHelper;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public class AppWindowsLock {

    private static final int GWLP_WNDPROC = -4;

    // Prevent GC
    private static final WinLockMsgProc PROC = new WinLockMsgProc();

    public static void registerHook(WinDef.HWND hwnd) {
        try {
            int windowThreadID = User32.INSTANCE.GetWindowThreadProcessId(hwnd, null);
            if (windowThreadID == 0) {
                return;
            }

            Wtsapi32.INSTANCE.WTSRegisterSessionNotification(hwnd, Wtsapi32.NOTIFY_FOR_ALL_SESSIONS);
            PROC.oldWindowProc =
                    User32.INSTANCE.GetWindowLongPtr(hwnd, GWLP_WNDPROC).toPointer();
            User32Ex.INSTANCE.SetWindowLongPtr(hwnd, GWLP_WNDPROC, PROC);
        } catch (Throwable t) {
            ErrorEventFactory.fromThrowable(t).omit().handle();
        }
    }

    public interface WinMsgProc extends StdCallLibrary.StdCallCallback {

        @SuppressWarnings("unused")
        WinDef.LRESULT callback(WinDef.HWND hwnd, int uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam);
    }

    @Setter
    @RequiredArgsConstructor
    public static final class WinLockMsgProc implements WinMsgProc {

        private Pointer oldWindowProc;

        @Override
        public WinDef.LRESULT callback(WinDef.HWND hwnd, int uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
            // The awt UserSessionListener does not work, so do it manually
            if (uMsg == WinUser.WM_SESSION_CHANGE) {
                var type = wParam.longValue();
                TrackEvent.withInfo("Received WM_SESSION_CHANGE event with lock state")
                        .tag("type", type)
                        .handle();
                if (type == Wtsapi32.WTS_SESSION_LOCK) {
                    if (AppPrefs.get() != null) {
                        var b = AppPrefs.get().hibernateBehaviour().getValue();
                        if (b != null) {
                            ThreadHelper.runAsync(() -> {
                                b.runOnSleep();
                                b.runOnWake();
                            });
                            return new WinDef.LRESULT(0);
                        }
                    }
                }
            }

            return User32.INSTANCE.CallWindowProc(oldWindowProc, hwnd, uMsg, wParam, lParam);
        }
    }

    public interface User32Ex extends W32APIOptions {

        User32Ex INSTANCE = Native.load("user32", User32Ex.class, DEFAULT_OPTIONS);

        int SetWindowLongPtr(WinDef.HWND hWnd, int nIndex, WinMsgProc callback);
    }
}
