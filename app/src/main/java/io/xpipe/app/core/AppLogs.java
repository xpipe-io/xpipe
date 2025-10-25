package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.Deobfuscator;

import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.NOPLogger;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class AppLogs {

    public static final List<String> LOG_LEVELS = List.of("error", "warn", "info", "debug", "trace");

    private static final DateTimeFormatter NAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter MESSAGE_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss:SSS").withZone(ZoneId.systemDefault());

    private static AppLogs INSTANCE;

    @Getter
    private final PrintStream originalSysOut;

    @Getter
    private final PrintStream originalSysErr;

    private final Path logDir;

    @Getter
    private final boolean writeToSysout;

    @Getter
    private final boolean writeToFile;

    @Getter
    private final String logLevel;

    private final PrintStream outFileStream;

    public AppLogs(
            Path logDir, boolean writeToSysout, boolean writeToFile, String logLevel, PrintStream outFileStream) {
        this.logDir = logDir;
        this.writeToSysout = writeToSysout;
        this.writeToFile = writeToFile;
        this.logLevel = logLevel;
        this.outFileStream = outFileStream;

        this.originalSysOut = System.out;
        this.originalSysErr = System.err;

        setLogLevels();
        hookUpSystemOut();
        hookUpSystemErr();
    }

    public static void init() {
        if (INSTANCE != null) {
            return;
        }

        var logDir = AppProperties.get().getDataDir().resolve("logs");

        // Regularly clean logs dir
        if (AppProperties.get().isNewBuildSession() && Files.exists(logDir)) {
            try {
                List<Path> all;
                try (var s = Files.list(logDir)) {
                    all = s.toList();
                }
                for (Path path : all) {
                    // Don't delete installer logs
                    if (path.getFileName().toString().contains("installer")) {
                        continue;
                    }

                    FileUtils.forceDelete(path.toFile());
                }
            } catch (Exception ex) {
                // It can happen that another instance is running that is locking a log file
                // Since we initialized before checking for another instance, this might fail
                ErrorEventFactory.fromThrowable(ex).expected().omit().handle();
            }
        }

        var now = Instant.now();
        var name = NAME_FORMATTER.format(now);
        Path usedLogsDir = logDir.resolve(name);

        // When two instances are being launched within the same second, add milliseconds
        if (Files.exists(usedLogsDir)) {
            usedLogsDir = logDir.resolve(name + "_" + now.get(ChronoField.MILLI_OF_SECOND));
        }

        PrintStream outFileStream = null;
        var shouldLogToFile = AppProperties.get().isLogToFile();
        if (shouldLogToFile) {
            try {
                FileUtils.forceMkdir(usedLogsDir.toFile());
                var file = usedLogsDir.resolve(AppNames.ofMain().getKebapName() + ".log");
                var fos = new FileOutputStream(file.toFile(), true);
                var buf = new BufferedOutputStream(fos);
                outFileStream = new PrintStream(buf, false);
            } catch (Exception ex) {
                ErrorEventFactory.fromThrowable(ex).build().handle();
            }
        }

        var shouldLogToSysout = AppProperties.get().isLogToSysOut();

        if (shouldLogToFile && outFileStream == null) {
            TrackEvent.info("Log file initialization failed. Writing to standard out");
            shouldLogToSysout = true;
            shouldLogToFile = false;
        }

        if (shouldLogToFile && !shouldLogToSysout) {
            TrackEvent.info("Writing log output to " + usedLogsDir + " from now on");
        }

        var level = AppProperties.get().getLogLevel();
        INSTANCE = new AppLogs(usedLogsDir, shouldLogToSysout, shouldLogToFile, level, outFileStream);
    }

    public static void teardown() {
        if (AppLogs.get() == null) {
            return;
        }

        AppLogs.get().close();
        INSTANCE = null;
    }

    public static AppLogs get() {
        return INSTANCE;
    }

    public void flush() {
        if (outFileStream != null) {
            outFileStream.flush();
        }
    }

    private void close() {
        if (outFileStream != null) {
            outFileStream.close();
        }
    }

    private void hookUpSystemOut() {
        System.setOut(new PrintStream(new OutputStream() {
            private final ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);

            @Override
            public void write(int b) {
                if (b == '\r' || b == '\n') {
                    String line = baos.toString();
                    if (line.length() == 0) {
                        return;
                    }

                    TrackEvent.builder().type("info").message(line).build().handle();
                    baos.reset();
                } else {
                    baos.write(b);
                }
            }
        }));
    }

    private void hookUpSystemErr() {
        System.setErr(new PrintStream(new OutputStream() {
            private final ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);

            @Override
            public void write(int b) {
                if (b == '\r' || b == '\n') {
                    String line = baos.toString();
                    if (line.length() == 0) {
                        return;
                    }

                    TrackEvent.builder().type("error").message(line).build().handle();
                    baos.reset();
                } else {
                    baos.write(b);
                }
            }
        }));
    }

    public void logException(String description, Throwable e) {
        var deob = Deobfuscator.deobfuscateToString(e);
        var event = TrackEvent.builder()
                .type("error")
                .message((description != null ? description : "") + "\n" + deob)
                .build();
        logEvent(event);
    }

    public synchronized void logEvent(TrackEvent event) {
        var li = LOG_LEVELS.indexOf(AppProperties.get().getLogLevel());
        int i = li == -1 ? 5 : li;
        int current = LOG_LEVELS.indexOf(event.getType());
        if (current <= i) {
            if (writeToSysout) {
                logSysOut(event);
            }
            if (writeToFile) {
                logToFile(event);
            }
        }
    }

    private synchronized void logSysOut(TrackEvent event) {
        var time = MESSAGE_FORMATTER.format(event.getInstant());
        var string =
                new StringBuilder(time).append(" - ").append(event.getType()).append(": ");
        string.append(event);
        var toLog = string.toString();
        this.originalSysOut.println(toLog);
    }

    private void logToFile(TrackEvent event) {
        var time = MESSAGE_FORMATTER.format(event.getInstant());
        var string =
                new StringBuilder(time).append(" - ").append(event.getType()).append(": ");
        string.append(event);
        var toLog = string.toString();
        outFileStream.println(toLog);
    }

    private void setLogLevels() {
        // Debug output for platform
        if (AppProperties.get().isLogPlatformDebug()) {
            System.setProperty("prism.verbose", "true");
            System.setProperty("prism.debug", "true");
            // System.setProperty("prism.trace", "true");
            // System.setProperty("sun.perflog", "results.log");
            //            System.setProperty("quantum.verbose", "true");
            //            System.setProperty("quantum.debug", "true");
            //            System.setProperty("quantum.pulse", "true");
        }
    }

    public Path getSessionLogsDirectory() {
        return logDir;
    }

    public static final class Slf4jProvider implements SLF4JServiceProvider {

        private static final String REQUESTED_API_VERSION = "2.0";

        private final ILoggerFactory factory = new ILoggerFactory() {

            private final Map<String, Logger> loggers = new ConcurrentHashMap<>();

            public Logger getLogger(String name) {
                // Only change this when debugging the logs of other libraries
                return NOPLogger.NOP_LOGGER;

                //                                // Don't use fully qualified class names
                //                                var normalizedName = FilenameUtils.getExtension(name);
                //                                if (normalizedName == null || normalizedName.isEmpty()) {
                //                                    normalizedName = name;
                //                                }
                //
                //                                return loggers.computeIfAbsent(normalizedName, s -> new
                // Slf4jLogger());
            }
        };

        @Override
        public ILoggerFactory getLoggerFactory() {
            return factory;
        }

        @Override
        public IMarkerFactory getMarkerFactory() {
            return null;
        }

        @Override
        public MDCAdapter getMDCAdapter() {
            return null;
        }

        @Override
        public String getRequestedApiVersion() {
            return REQUESTED_API_VERSION;
        }

        @Override
        public void initialize() {}
    }

    public static final class Slf4jLogger extends AbstractLogger {

        @Override
        protected String getFullyQualifiedCallerName() {
            return "logger";
        }

        @Override
        protected void handleNormalizedLoggingCall(
                Level level, Marker marker, String msg, Object[] arguments, Throwable throwable) {
            if (arguments != null) {
                for (var arg : arguments) {
                    msg = msg.replaceFirst("\\{}", Objects.toString(arg));
                }
            }
            TrackEvent.builder()
                    .type(level.toString().toLowerCase())
                    .message(msg)
                    .build()
                    .handle();
        }

        @Override
        public boolean isTraceEnabled() {
            // return LOG_LEVELS.indexOf("trace") <= LOG_LEVELS.indexOf(AppLogs.get().getLogLevel());

            // You almost never want trace output, javafx will spam everything
            return false;
        }

        @Override
        public boolean isTraceEnabled(Marker marker) {
            // return LOG_LEVELS.indexOf("trace") <= LOG_LEVELS.indexOf(AppLogs.get().getLogLevel());

            // You almost never want trace output, javafx will spam everything
            return false;
        }

        @Override
        public boolean isDebugEnabled() {
            return LOG_LEVELS.indexOf("debug")
                    <= LOG_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }

        @Override
        public boolean isDebugEnabled(Marker marker) {
            return LOG_LEVELS.indexOf("debug")
                    <= LOG_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }

        @Override
        public boolean isInfoEnabled() {
            return LOG_LEVELS.indexOf("info")
                    <= LOG_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }

        @Override
        public boolean isInfoEnabled(Marker marker) {
            return LOG_LEVELS.indexOf("info")
                    <= LOG_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }

        @Override
        public boolean isWarnEnabled() {
            return LOG_LEVELS.indexOf("warn")
                    <= LOG_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }

        @Override
        public boolean isWarnEnabled(Marker marker) {
            return LOG_LEVELS.indexOf("warn")
                    <= LOG_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }

        @Override
        public boolean isErrorEnabled() {
            return LOG_LEVELS.indexOf("error")
                    <= LOG_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }

        @Override
        public boolean isErrorEnabled(Marker marker) {
            return LOG_LEVELS.indexOf("error")
                    <= LOG_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }
    }
}
