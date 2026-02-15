package io.xpipe.app.platform;

import io.xpipe.app.util.Rect;

import io.xpipe.app.util.User32Ex;
import javafx.stage.Window;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.platform.win32.*;
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

    private static final int WS_EX_APPWINDOW = 0x00040000;

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

    public void removeBorders() {
        var rect = getBounds();

        var style = User32.INSTANCE.GetWindowLong(windowHandle, User32.GWL_STYLE);
        var mod = style & ~(User32.WS_CAPTION | User32.WS_THICKFRAME | User32.WS_MAXIMIZEBOX);
        User32.INSTANCE.SetWindowLong(windowHandle, User32.GWL_STYLE, mod);

        User32.INSTANCE.SetWindowPos(windowHandle, null, rect.getX(), rect.getY(), rect.getW() + 1, rect.getH(),
                User32.SWP_NOACTIVATE | User32.SWP_NOMOVE | User32.SWP_NOZORDER);
        User32.INSTANCE.SetWindowPos(windowHandle, null, rect.getX(), rect.getY(), rect.getW(), rect.getH(),
                User32.SWP_NOACTIVATE | User32.SWP_NOMOVE | User32.SWP_NOZORDER);
    }

    public void restoreBorders() {
        var rect = getBounds();

        var style = User32.INSTANCE.GetWindowLong(windowHandle, User32.GWL_STYLE);
        var mod = style | User32.WS_CAPTION | User32.WS_THICKFRAME | User32.WS_MAXIMIZEBOX;
        User32.INSTANCE.SetWindowLong(windowHandle, User32.GWL_STYLE, mod);

        User32.INSTANCE.SetWindowPos(windowHandle, null, rect.getX(), rect.getY(), rect.getW() + 1, rect.getH(),
                User32.SWP_NOACTIVATE | User32.SWP_NOMOVE | User32.SWP_NOZORDER);
        User32.INSTANCE.SetWindowPos(windowHandle, null, rect.getX(), rect.getY(), rect.getW(), rect.getH(),
                User32.SWP_NOACTIVATE | User32.SWP_NOMOVE | User32.SWP_NOZORDER);
    }

    public void takeOwnership(WinDef.HWND owner) {
        var style = User32.INSTANCE.GetWindowLong(windowHandle, User32.GWL_EXSTYLE);
        var mod = style & ~(WS_EX_APPWINDOW);
        User32.INSTANCE.SetWindowLong(windowHandle, User32.GWL_EXSTYLE, mod);

        setWindowsTransitionsEnabled(false);

        User32Ex.INSTANCE.SetWindowLongPtr(getWindowHandle(), User32.GWL_HWNDPARENT, owner);
    }

    public void releaseOwnership() {
        var style = User32.INSTANCE.GetWindowLong(windowHandle, User32.GWL_EXSTYLE);
        var mod = style | WS_EX_APPWINDOW;
        User32.INSTANCE.SetWindowLong(windowHandle, User32.GWL_EXSTYLE, mod);

        setWindowsTransitionsEnabled(true);

        User32Ex.INSTANCE.SetWindowLongPtr(getWindowHandle(), User32.GWL_HWNDPARENT, (WinDef.HWND) null);
    }

    public boolean isIconified() {
        return (User32.INSTANCE.GetWindowLong(windowHandle, User32.GWL_STYLE) & User32.WS_MINIMIZE) != 0;
    }

    public boolean isVisible() {
        return User32.INSTANCE.IsWindowVisible(windowHandle);
    }

    public void moveToFront() {
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
                windowHandle, null, bounds.getX(), bounds.getY(), bounds.getW(), bounds.getH(), User32.SWP_NOACTIVATE | User32.SWP_NOZORDER);
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

    public void setWindowsTransitionsEnabled(boolean enabled) {
        setWindowAttribute(DmwaWindowAttribute.DWMWA_TRANSITIONS_FORCEDISABLED.get(), !enabled);
    }

    public enum DmwaWindowAttribute {
        DWMWA_TRANSITIONS_FORCEDISABLED(3),
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
