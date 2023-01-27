package io.xpipe.cli.util;

import io.xpipe.core.dialog.DialogCancelException;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.XPipeInstallation;
import org.graalvm.nativeimage.ImageInfo;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class CliHelper {

    private static final String DEBUG_PROP = "io.xpipe.cli.debug";

    public static String readLine() throws DialogCancelException {
        try {
            Scanner scanner = new Scanner(System.in);
            var read = scanner.nextLine();
            if (read.length() > 0) {
                return read;
            }

            return null;
        } catch (NoSuchElementException ex) {
            throw new DialogCancelException();
        }
    }

    public static boolean isProduction() {
        return ImageInfo.isExecutable();
    }

    public static String findInstallation() throws Exception {
        return XPipeInstallation.getLocalInstallationBasePathForCLI(CliHelper.getExecutableLocation());
    }

    public static String findDaemonExecutable() throws Exception {
        var base = findInstallation();
        return FileNames.join(base, XPipeInstallation.getDaemonExecutablePath(OsType.getLocal()));
    }

    public static String getExecutableLocation() {
        if (!isProduction()) {
            return null;
        }

        try {
            return Path.of(CliHelper.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI())
                    .toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean shouldPrintStackTrace() {
        if (System.getProperty(DEBUG_PROP) != null) {
            return Boolean.parseBoolean(System.getProperty(DEBUG_PROP));
        }
        return false;
    }

    public static boolean hasPipedInput() {
        var in = System.in;
        try {
            if (in.available() == 0) {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }

        return true;
    }

    public static boolean canHaveUserInput() {
        if (!isProduction()) {
            return true;
        }

        return System.console() != null;
    }

    public static int getConsoleWidth() {
        return 80;
    }
}
