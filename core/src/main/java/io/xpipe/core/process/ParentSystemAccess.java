package io.xpipe.core.process;

public interface ParentSystemAccess {

    static ParentSystemAccess none() {
        return new ParentSystemAccess() {
            @Override
            public boolean supportsFileSystemAccess() {
                return false;
            }

            @Override
            public boolean supportsExecutables() {
                return false;
            }

            @Override
            public boolean supportsExecutableEnvironment() {
                return false;
            }

            @Override
            public String translateFromLocalSystemPath(String path) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String translateToLocalSystemPath(String path) {
                throw new UnsupportedOperationException();
            }
        };
    }

    static ParentSystemAccess identity() {
        return new ParentSystemAccess() {

            @Override
            public boolean supportsFileSystemAccess() {
                return true;
            }

            @Override
            public boolean supportsExecutables() {
                return true;
            }

            @Override
            public boolean supportsExecutableEnvironment() {
                return true;
            }

            @Override
            public String translateFromLocalSystemPath(String path) {
                return path;
            }

            @Override
            public String translateToLocalSystemPath(String path) {
                return path;
            }
        };
    }

    static ParentSystemAccess combine(ParentSystemAccess a1, ParentSystemAccess a2) {
        return new ParentSystemAccess() {

            @Override
            public boolean supportsFileSystemAccess() {
                return a1.supportsFileSystemAccess() && a2.supportsFileSystemAccess();
            }

            @Override
            public boolean supportsExecutables() {
                return a1.supportsExecutables() && a2.supportsExecutables();
            }

            @Override
            public boolean supportsExecutableEnvironment() {
                return a1.supportsExecutableEnvironment() && a2.supportsExecutableEnvironment();
            }

            @Override
            public String translateFromLocalSystemPath(String path) throws Exception {
                return a2.translateFromLocalSystemPath(a1.translateFromLocalSystemPath(path));
            }

            @Override
            public String translateToLocalSystemPath(String path) throws Exception {
                return a1.translateToLocalSystemPath(a2.translateToLocalSystemPath(path));
            }
        };
    }

    default boolean supportsAnyAccess() {
        return supportsFileSystemAccess();
    }

    boolean supportsFileSystemAccess();

    boolean supportsExecutables();

    boolean supportsExecutableEnvironment();

    String translateFromLocalSystemPath(String path) throws Exception;

    String translateToLocalSystemPath(String path) throws Exception;
}
