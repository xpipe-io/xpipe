package io.xpipe.app.util;

import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ModuleAccess {

    public static void exportAndOpen(Module source, String pkg, Module target) throws Exception {
        if (source.isExported(pkg, target) && source.isOpen(pkg, target)) {
            return;
        }

        Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredMethods0", boolean.class);
        getDeclaredFields0.setAccessible(true);
        Method[] fields = (Method[]) getDeclaredFields0.invoke(Module.class, false);
        Method modifiers = null;
        for (Method each : fields) {
            if ("implAddExportsOrOpens".equals(each.getName())) {
                modifiers = each;
                break;
            }
        }
        modifiers.setAccessible(true);

        modifiers.invoke(source, pkg, target, false, true);
        modifiers.invoke(source, pkg, target, true, true);
    }
}