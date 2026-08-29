package io.xpipe.app.process;

import io.xpipe.app.util.FilePath;

public interface WorkingDirectoryFunction {

    static WorkingDirectoryFunction fixed(FilePath path) {
        return new WorkingDirectoryFunction() {
            @Override
            public boolean isFixed() {
                return true;
            }

            @Override
            public boolean isSpecified() {
                return true;
            }

            @Override
            public FilePath apply(ShellControl shellControl) {
                return path;
            }
        };
    }

    static WorkingDirectoryFunction none() {
        return new WorkingDirectoryFunction() {
            @Override
            public boolean isFixed() {
                return true;
            }

            @Override
            public boolean isSpecified() {
                return false;
            }

            @Override
            public FilePath apply(ShellControl shellControl) {
                return null;
            }
        };
    }

    boolean isFixed();

    boolean isSpecified();

    FilePath apply(ShellControl shellControl);
}
