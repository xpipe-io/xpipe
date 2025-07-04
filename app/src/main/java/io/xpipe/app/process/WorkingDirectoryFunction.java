package io.xpipe.app.process;

import io.xpipe.core.FailableFunction;
import io.xpipe.core.FilePath;

public interface WorkingDirectoryFunction {

    static WorkingDirectoryFunction of(FailableFunction<ShellControl, FilePath, Exception> path) {
        return new WorkingDirectoryFunction() {
            @Override
            public boolean isFixed() {
                return false;
            }

            @Override
            public boolean isSpecified() {
                return true;
            }

            @Override
            public FilePath apply(ShellControl shellControl) throws Exception {
                return path.apply(shellControl);
            }
        };
    }

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

    FilePath apply(ShellControl shellControl) throws Exception;
}
