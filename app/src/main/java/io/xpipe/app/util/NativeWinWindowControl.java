package io.xpipe.app.util;

import javafx.stage.Window;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Getter
@EqualsAndHashCode
public class NativeWinWindowControl {

    public static NativeWinWindowControl MAIN_WINDOW;
    private final WinDef.HWND windowHandle;

    @SneakyThrows
    public NativeWinWindowControl(Window stage) {
        this.windowHandle = byWindow(stage);
    }

    public NativeWinWindowControl(WinDef.HWND windowHandle) {
        this.windowHandle = windowHandle;
    }

    @SneakyThrows
    public static WinDef.HWND byWindow(Window window) {
        Method tkStageGetter = Window.class.getDeclaredMethod("getPeer");
        tkStageGetter.setAccessible(true);
        Object tkStage = tkStageGetter.invoke(window);
        Method getPlatformWindow = tkStage.getClass().getDeclaredMethod("getPlatformWindow");
        getPlatformWindow.setAccessible(true);
        Object platformWindow = getPlatformWindow.invoke(tkStage);
        Method getNativeHandle = platformWindow.getClass().getMethod("getNativeHandle");
        getNativeHandle.setAccessible(true);
        Object nativeHandle = getNativeHandle.invoke(platformWindow);
        var hwnd = new WinDef.HWND(new Pointer((long) nativeHandle));
        return hwnd;
    }

    public static List<NativeWinWindowControl> byPid(long pid) {
        var refs = new ArrayList<NativeWinWindowControl>();
        User32.INSTANCE.EnumWindows(
                (hWnd, data) -> {
                    var visible = User32.INSTANCE.IsWindowVisible(hWnd);
                    if (!visible) {
                        return true;
                    }

                    var wpid = new IntByReference();
                    User32.INSTANCE.GetWindowThreadProcessId(hWnd, wpid);
                    if (wpid.getValue() == pid) {
                        refs.add(new NativeWinWindowControl(hWnd));
                    }
                    return true;
                },
                null);
        return refs;
    }

    public boolean isIconified() {
        return (User32.INSTANCE.GetWindowLong(windowHandle, User32.GWL_STYLE) & User32.WS_MINIMIZE) != 0;
    }

    public boolean isVisible() {
        return User32.INSTANCE.IsWindowVisible(windowHandle);
    }

    public void alwaysInFront() {
        orderRelative(new WinDef.HWND(new Pointer(0xFFFFFFFFFFFFFFFFL)));
    }

    public void defaultOrder() {
        orderRelative(new WinDef.HWND(new Pointer(-2)));
        orderRelative(new WinDef.HWND(new Pointer(0)));
    }

    public void orderRelative(WinDef.HWND predecessor) {
        User32.INSTANCE.SetWindowPos(
                windowHandle, predecessor, 0, 0, 0, 0, User32.SWP_NOACTIVATE | User32.SWP_NOMOVE | User32.SWP_NOSIZE);
    }

    public void show() {
        User32.INSTANCE.ShowWindow(windowHandle, User32.SW_RESTORE);
    }

    public void close() {
        User32.INSTANCE.PostMessage(windowHandle, User32.WM_CLOSE, null, null);
    }

    public void minimize() {
        User32.INSTANCE.ShowWindow(windowHandle, User32.SW_MINIMIZE);
    }

    public void move(Rect bounds) {
        User32.INSTANCE.SetWindowPos(
                windowHandle, null, bounds.getX(), bounds.getY(), bounds.getW(), bounds.getH(), User32.SWP_NOACTIVATE);
    }

    public Rect getBounds() {
        var rect = new WinDef.RECT();
        User32.INSTANCE.GetWindowRect(windowHandle, rect);
        return new Rect(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
    }

    public boolean setWindowAttribute(int attribute, boolean attributeValue) {
        var r = Dwm.INSTANCE.DwmSetWindowAttribute(
                windowHandle, attribute, new WinDef.BOOLByReference(new WinDef.BOOL(attributeValue)), WinDef.BOOL.SIZE);
        return r.longValue() == 0;
    }

    public void activate() {
        User32.INSTANCE.SetForegroundWindow(windowHandle);
    }

    public boolean setWindowBackdrop(DwmSystemBackDropType backdrop) {
        var r = Dwm.INSTANCE.DwmSetWindowAttribute(
                windowHandle,
                DmwaWindowAttribute.DWMWA_SYSTEMBACKDROP_TYPE.get(),
                new WinDef.DWORDByReference(new WinDef.DWORD(backdrop.get())),
                WinDef.DWORD.SIZE);
        return r.longValue() == 0;
    }

    public enum DmwaWindowAttribute {
        DWMWA_USE_IMMERSIVE_DARK_MODE(20),
        DWMWA_SYSTEMBACKDROP_TYPE(38);

        private final int value;

        DmwaWindowAttribute(int value) {
            this.value = value;
        }

        public int get() {
            return value;
        }
    }

    @SuppressWarnings("unused")
    public enum DwmSystemBackDropType {
        // DWMSBT_NONE
        NONE(1),
        // DWMSBT_MAINWINDOW
        MICA(2),
        // DWMSBT_TRANSIENTWINDOW
        ACRYLIC(3),
        // DWMSBT_TABBEDWINDOW
        MICA_ALT(4);

        private final int value;

        DwmSystemBackDropType(int value) {
            this.value = value;
        }

        public int get() {
            return value;
        }
    }

    public interface Dwm extends Library {

        Dwm INSTANCE = Native.load("dwmapi", Dwm.class);

        WinNT.HRESULT DwmSetWindowAttribute(
                WinDef.HWND hwnd, int dwAttribute, PointerType pvAttribute, int cbAttribute);
    }
}
