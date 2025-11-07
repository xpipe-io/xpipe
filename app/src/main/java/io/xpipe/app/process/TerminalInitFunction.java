package io.xpipe.app.process;

import io.xpipe.core.FailableFunction;

public interface TerminalInitFunction {

    static TerminalInitFunction of(FailableFunction<ShellControl, String, Exception> f) {
        return new TerminalInitFunction() {
            @Override
            public boolean isSpecified() {
                return true;
            }

            @Override
            public String apply(ShellControl shellControl) throws Exception {
                return f.apply(shellControl);
            }
        };
    }

    static TerminalInitFunction fixed(String s) {
        return new TerminalInitFunction() {
            @Override
            public boolean isSpecified() {
                return true;
            }

            @Override
            public String apply(ShellControl shellControl) {
                return s;
            }
        };
    }

    static TerminalInitFunction none() {
        return new TerminalInitFunction() {
            @Override
            public boolean isSpecified() {
                return false;
            }

            @Override
            public String apply(ShellControl shellControl) {
                return null;
            }
        };
    }

    boolean isSpecified();

    String apply(ShellControl shellControl) throws Exception;
}
