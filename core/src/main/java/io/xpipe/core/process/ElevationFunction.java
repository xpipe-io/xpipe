package io.xpipe.core.process;

import io.xpipe.core.util.FailableFunction;

public interface ElevationFunction {

    static ElevationFunction ifNotRoot(ElevationFunction function) {
        return new ElevationFunction() {
            @Override
            public String getPrefix() {
                return function.getPrefix();
            }

            @Override
            public boolean isSpecified() {
                return true;
            }

            @Override
            public boolean apply(ShellControl shellControl) throws Exception {
                if (shellControl.getOsType() == OsType.WINDOWS) {
                    return false;
                }

                var isRoot = shellControl.executeSimpleBooleanCommand("/usr/bin/test \"${EUID:-$(id -u)}\" -eq 0");
                if (isRoot) {
                    return false;
                }

                return function.apply(shellControl);
            }
        };
    }

    static ElevationFunction of(String prefix, FailableFunction<ShellControl, Boolean, Exception> f) {
        return new ElevationFunction() {
            @Override
            public String getPrefix() {
                return prefix;
            }

            @Override
            public boolean isSpecified() {
                return true;
            }

            @Override
            public boolean apply(ShellControl shellControl) throws Exception {
                return f.apply(shellControl);
            }
        };
    }

    static ElevationFunction elevated(String prefix) {
        return new ElevationFunction() {
            @Override
            public String getPrefix() {
                return prefix;
            }

            @Override
            public boolean isSpecified() {
                return true;
            }

            @Override
            public boolean apply(ShellControl shellControl) {
                return true;
            }
        };
    }

    static ElevationFunction none() {
        return new ElevationFunction() {
            @Override
            public String getPrefix() {
                return null;
            }

            @Override
            public boolean isSpecified() {
                return false;
            }

            @Override
            public boolean apply(ShellControl shellControl) {
                return false;
            }
        };
    }

    String getPrefix();

    boolean isSpecified();

    boolean apply(ShellControl shellControl) throws Exception;
}
