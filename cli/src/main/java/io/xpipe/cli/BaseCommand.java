package io.xpipe.cli;

import io.xpipe.beacon.BeaconException;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.ConnectorException;
import io.xpipe.beacon.ServerException;
import io.xpipe.cli.util.CliHelper;
import io.xpipe.cli.util.XPipeCliConnection;
import picocli.CommandLine;

import java.util.concurrent.Callable;

public abstract class BaseCommand implements Callable<Integer> {

    private int handleException(Throwable e) {
        var usedException = e instanceof BeaconException ? e.getCause() : e;
        if (usedException instanceof ConnectorException ce) {
            if (CliHelper.isProduction()) {
                System.err.println("A connection error occurred: " + ce.getMessage());
                if (CliHelper.shouldPrintStackTrace()) {
                    ce.printStackTrace();
                }
            } else {
                ce.printStackTrace();
            }
            return 3;
        }

        if (usedException instanceof ClientException ce) {
            if (CliHelper.isProduction()) {
                System.err.println(ce.getMessage());
                if (CliHelper.shouldPrintStackTrace()) {
                    ce.printStackTrace();
                }
            } else {
                ce.printStackTrace();
            }
            return 1;
        }

        if (usedException instanceof ServerException se) {
            if (CliHelper.isProduction()) {
                System.err.println("An internal xpipe error occurred: " + se.getMessage());
                if (CliHelper.shouldPrintStackTrace()) {
                    se.printStackTrace();
                }
            } else {
                se.printStackTrace();
            }
            return 2;
        }

        if (CliHelper.isProduction()) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            if (CliHelper.shouldPrintStackTrace()) {
                e.printStackTrace();
            }
        } else {
            e.printStackTrace();
        }
        return 3;
    }

    @Override
    public Integer call() throws Exception {
        boolean stopManually = false;
        try (var con = XPipeCliConnection.open()) {
            stopManually = con.isStopDaemonOnExit();
            execute(con);
            return 0;
        } catch (Exception ex) {
            if (ex instanceof BeaconException be && be.getCause() != null) {
                return handleException(be.getCause());
            }

            return handleException(ex);
        } finally {
            if (stopManually) {
                try (var con = XPipeCliConnection.open()) {
                    con.stopDaemon();
                }
            }
        }
    }

    protected String highlight(String in) {
        return CommandLine.Help.Ansi.AUTO.string("@|yellow " + in + "|@");
    }

    protected String header(String in) {
        return CommandLine.Help.Ansi.AUTO.string("@|bold " + in + "|@");
    }

    protected abstract void execute(XPipeCliConnection con) throws Exception;
}
