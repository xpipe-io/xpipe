package io.xpipe.app.util;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.W32APIOptions;
import io.xpipe.app.core.AppWindowsLock;

public interface User32Ex extends W32APIOptions {

    User32Ex INSTANCE = Native.load("user32", User32Ex.class, DEFAULT_OPTIONS);

    int SetWindowLongPtr(WinDef.HWND hWnd, int nIndex, AppWindowsLock.WinMsgProc callback);

    int SetWindowLongPtr(WinDef.HWND hWnd, int nIndex, WinDef.HWND w);
}
