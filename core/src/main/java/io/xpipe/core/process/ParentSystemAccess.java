package io.xpipe.core.process;

public interface ParentSystemAccess {

    static ParentSystemAccess none() {
        return new ParentSystemAccess() {
            @Override
            public boolean supportsSameUsers() {
                return false;
            }

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

            @Override
            public boolean isIdentity() {
                return false;
            }
        };
    }

    static ParentSystemAccess identity() {
        return new ParentSystemAccess() {
            @Override
            public boolean supportsSameUsers() {
                return true;
            }

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

            @Override
            public boolean isIdentity() {
                return true;
            }
        };
    }

    static ParentSystemAccess combine(ParentSystemAccess a1, ParentSystemAccess a2) {
        return new ParentSystemAccess() {
            @Override
            public boolean supportsSameUsers() {
                return a1.supportsSameUsers() && a2.supportsSameUsers();
            }

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

            @Override
            public boolean isIdentity() {
                return a1.isIdentity() && a2.isIdentity();
            }
        };
    }

    default boolean supportsAnyAccess() {
        return supportsFileSystemAccess();
    }

    boolean supportsSameUsers();

    boolean supportsFileSystemAccess();

    boolean supportsExecutables();

    boolean supportsExecutableEnvironment();

    String translateFromLocalSystemPath(String path) throws Exception;

    String translateToLocalSystemPath(String path) throws Exception;

    boolean isIdentity();
}
