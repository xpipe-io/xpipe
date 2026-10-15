package io.xpipe.app.process;

import io.xpipe.app.ext.ProcModuleProvider;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class LocalShellPool {

    private static final int SIZE = 10;
    private static final List<ShellControl> shells = new ArrayList<>();
    private static final Semaphore permits = new Semaphore(SIZE, true);

    public static PooledShell get() throws Exception {
        permits.acquire();

        ShellControl sc;
        try {
            sc = reserve();
        } catch (Exception e) {
            permits.release();
            throw e;
        }

        try {
            sc.start();
        } catch (Exception e) {
            synchronized (LocalShellPool.class) {
                shells.remove(sc);
            }
            sc.getLock().unlock();
            permits.release();
            throw e;
        }
        return new PooledShell(sc);
    }

    private static synchronized ShellControl reserve() {
        for (ShellControl shell : shells) {
            if (shell.getLock().tryLock()) {
                return shell;
            }
        }

        var sc = ProcModuleProvider.get().createLocalProcessControl(true);
        sc.getLock().lock();
        shells.add(sc);
        return sc;
    }

    public static final class PooledShell implements AutoCloseable {

        @Getter
        private final ShellControl shell;

        private boolean closed;

        private PooledShell(ShellControl shell) {
            this.shell = shell;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }

            closed = true;
            shell.getLock().unlock();
            permits.release();
        }
    }
}
