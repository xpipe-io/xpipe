package io.xpipe.app.core;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.PlatformState;
import io.xpipe.app.util.ThreadHelper;

import com.sun.jna.*;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

public class AppWindowsShutdown {

    // Prevent GC
    private static final WinShutdownHookProc PROC = new WinShutdownHookProc();

    public static void registerHook(WinDef.HWND hwnd) {
        try {
            int windowThreadID = User32.INSTANCE.GetWindowThreadProcessId(hwnd, null);
            if (windowThreadID == 0) {
                return;
            }

            PROC.hwnd = hwnd;
            PROC.hhook = User32.INSTANCE.SetWindowsHookEx(4, PROC, null, windowThreadID);
        } catch (Throwable t) {
            ErrorEventFactory.fromThrowable(t).omit().handle();
        }
    }

    public static class CWPSSTRUCT extends Structure {
        public WinDef.LPARAM lParam;
        public WinDef.WPARAM wParam;
        public WinDef.DWORD message;
        public WinDef.HWND hwnd;

        @Override
        protected List<String> getFieldOrder() {
            return List.of("lParam", "wParam", "message", "hwnd");
        }
    }

    public interface WinHookProc extends WinUser.HOOKPROC {

        WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, CWPSSTRUCT hookProcStruct);
    }

    public static final int WM_ENDSESSION = 0x16;
    public static final int WM_QUERYENDSESSION = 0x11;
    public static final long ENDSESSION_CRITICAL = 0x40000000L;

    @Setter
    @RequiredArgsConstructor
    public static final class WinShutdownHookProc implements WinHookProc {

        private WinUser.HHOOK hhook;
        private WinDef.HWND hwnd;

        @Override
        public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, CWPSSTRUCT hookProcStruct) {
            if (nCode >= 0 && hookProcStruct.hwnd.equals(hwnd)) {
                if (hookProcStruct.message.longValue() == WM_QUERYENDSESSION) {
                    TrackEvent.info("Received window shutdown callback WM_QUERYENDSESSION");

                    // We don't always receive an exit signal with a queryendsession, e.g. in case an .msi wants to shut
                    // it down
                    // Guarantee that the shutdown is run regardless
                    ThreadHelper.runAsync(() -> {
                        OperationMode.externalShutdown();
                    });

                    // Indicates that we need to run the endsession case blocking
                    return new WinDef.LRESULT(0);
                }

                if (hookProcStruct.message.longValue() == WM_ENDSESSION) {
                    var type = hookProcStruct.lParam.longValue();
                    TrackEvent.withInfo("Received window shutdown callback WM_ENDSESSION")
                            .tag("type", type)
                            .handle();

                    // Instant exit for critical shutdowns
                    if (type == ENDSESSION_CRITICAL) {
                        OperationMode.halt(0);
                    }

                    // A shutdown hook will be started in parallel while we exit
                    // The only thing we have to do is wait for it to exit the platform
                    while (PlatformState.getCurrent() != PlatformState.EXITED) {
                        ThreadHelper.sleep(10);
                    }

                    TrackEvent.withInfo("Wait for shutdown for WM_ENDSESSION finished")
                            .tag("type", type)
                            .handle();

                    return new WinDef.LRESULT(0);
                }
            }
            return User32.INSTANCE.CallNextHookEx(
                    hhook, nCode, wParam, new WinDef.LPARAM(Pointer.nativeValue(hookProcStruct.getPointer())));
        }
    }
}
