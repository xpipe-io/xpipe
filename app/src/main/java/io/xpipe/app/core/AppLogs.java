package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.util.Deobfuscator;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class AppLogs {

    public static final List<String> DEFAULT_LEVELS = List.of("error", "warn", "info", "debug", "trace");
    private static final String WRITE_SYSOUT_PROP = "io.xpipe.app.writeSysOut";
    private static final String WRITE_LOGS_PROP = "io.xpipe.app.writeLogs";
    private static final String DEBUG_PLATFORM_PROP = "io.xpipe.app.debugPlatform";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter MESSAGE_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss:SSS").withZone(ZoneId.systemDefault());
    private static AppLogs INSTANCE;
    private final PrintStream originalSysOut;
    private final PrintStream originalSysErr;
    private final Path logDir;

    @Getter
    private final boolean writeToSysout;

    @Getter
    private final boolean writeToFile;

    private final PrintStream outStream;
    private final Map<String, PrintStream> categoryWriters;

    public AppLogs(Path logDir, boolean writeToSysout, boolean writeToFile) {
        this.logDir = logDir;
        this.writeToSysout = writeToSysout;
        this.writeToFile = writeToFile;
        this.outStream = System.out;
        this.categoryWriters = new HashMap<>();

        this.originalSysOut = System.out;
        this.originalSysErr = System.err;

        setLogLevels();
        hookUpSystemOut();
        hookUpSystemErr();
    }

    private static boolean shouldWriteLogs() {
        if (System.getProperty(WRITE_LOGS_PROP) != null) {
            return Boolean.parseBoolean(System.getProperty(WRITE_LOGS_PROP));
        }

        return false;
    }

    private static boolean shouldWriteSysout() {
        if (System.getProperty(WRITE_SYSOUT_PROP) != null) {
            return Boolean.parseBoolean(System.getProperty(WRITE_SYSOUT_PROP));
        }

        return false;
    }

    public static void init() {
        var logDir = AppProperties.get().getDataDir().resolve("logs");
        var shouldLogToFile = shouldWriteLogs();

        var now = Instant.now();
        var name = FORMATTER.format(now);
        Path usedLogsDir = logDir.resolve(name);

        // When two instances are being launched within the same second, add milliseconds
        if (Files.exists(usedLogsDir)) {
            usedLogsDir = logDir.resolve(name + "_" + now.get(ChronoField.MILLI_OF_SECOND));
        }

        if (shouldLogToFile) {
            try {
                Files.createDirectories(usedLogsDir);
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).build().handle();
                shouldLogToFile = false;
            }
        }

        var shouldLogToSysout = shouldWriteSysout();

        INSTANCE = new AppLogs(usedLogsDir, shouldLogToSysout, shouldLogToFile);
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

    private void close() {
        outStream.close();
        categoryWriters.forEach((k, s) -> {
            s.close();
        });
    }

    private String getCategory(TrackEvent event) {
        if (event.getCategory() != null) {
            return event.getCategory();
        }

        return "misc";
    }

    private synchronized PrintStream getLogStream(TrackEvent e) {
        return categoryWriters.computeIfAbsent(getCategory(e), (cat) -> {
            var file = logDir.resolve(cat + ".log");
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(file.toFile(), true);
            } catch (IOException ex) {
                return outStream;
            }
            return new PrintStream(fos, false);
        });
    }

    public synchronized PrintStream getCatchAllLogStream() {
        return categoryWriters.computeIfAbsent("xpipe", (cat) -> {
            var file = logDir.resolve(cat + ".log");
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(file.toFile(), true);
            } catch (IOException ex) {
                return outStream;
            }
            return new PrintStream(fos, false);
        });
    }

    private boolean shouldDebugPlatform() {
        if (System.getProperty(DEBUG_PLATFORM_PROP) != null) {
            return Boolean.parseBoolean(System.getProperty(DEBUG_PLATFORM_PROP));
        }

        return false;
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

                    TrackEvent.builder()
                            .type("info")
                            .category("sysout")
                            .message(line)
                            .build()
                            .handle();
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

    private String getLogLevel() {
        if (AppPrefs.get() == null) {
            return "trace";
        }

        return AppPrefs.get().logLevel().getValue();
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
        var li = DEFAULT_LEVELS.indexOf(getLogLevel());
        int i = li == -1 ? 5 : li;
        int current = DEFAULT_LEVELS.indexOf(event.getType());
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
        if (event.getCategory() != null) {
            string.append("[").append(event.getCategory()).append("] ");
        }
        string.append(event);
        var toLog = string.toString();
        outStream.println(toLog);
    }

    private void logToFile(TrackEvent event) {
        var time = MESSAGE_FORMATTER.format(event.getInstant());
        var string =
                new StringBuilder(time).append(" - ").append(event.getType()).append(": ");
        string.append(event);
        var toLog = string.toString();
        getLogStream(event).println(toLog);
        getCatchAllLogStream().println(toLog);
    }

    private void setLogLevels() {
        // Debug output for platform
        if (shouldDebugPlatform()) {
            System.setProperty("prism.verbose", "true");
            System.setProperty("prism.debug", "true");
            System.setProperty("prism.trace", "true");
            System.setProperty("sun.perflog", "results.log");
            //            System.setProperty("quantum.verbose", "true");
            //            System.setProperty("quantum.debug", "true");
            //            System.setProperty("quantum.pulse", "true");
        }
    }

    public PrintStream getOriginalSysOut() {
        return originalSysOut;
    }

    public PrintStream getOriginalSysErr() {
        return originalSysErr;
    }

    public Path getLogsDirectory() {
        return logDir.getParent();
    }

    public Path getSessionLogsDirectory() {
        return logDir;
    }

    public static final class Slf4jProvider implements SLF4JServiceProvider {

        private static final String REQUESTED_API_VERSION = "2.0";

        private final ILoggerFactory factory = new ILoggerFactory() {

            private final Map<String, Logger> loggers = new ConcurrentHashMap<>();

            public Logger getLogger(String name) {
                if (AppLogs.get() == null) {
                    return NOPLogger.NOP_LOGGER;
                }

                // Don't use fully qualified class names
                var normalizedName = FilenameUtils.getExtension(name);
                if (normalizedName == null || normalizedName.isEmpty()) {
                    normalizedName = name;
                }

                return loggers.computeIfAbsent(normalizedName, Slf4jLogger::new);
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

        private final String name;

        public Slf4jLogger(String name) {
            this.name = name;
        }

        @Override
        protected String getFullyQualifiedCallerName() {
            return "logger";
        }

        @Override
        protected void handleNormalizedLoggingCall(
                Level level, Marker marker, String msg, Object[] arguments, Throwable throwable) {
            var formatted = msg;
            if (arguments != null) {
                for (var arg : arguments) {
                    msg = msg.replaceFirst("\\{}", Objects.toString(arg));
                }
            }
            TrackEvent.builder()
                    .category(name)
                    .type(level.toString().toLowerCase())
                    .message(msg)
                    .build()
                    .handle();
        }

        @Override
        public boolean isTraceEnabled() {
            return DEFAULT_LEVELS.indexOf("trace")
                    <= DEFAULT_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }

        @Override
        public boolean isTraceEnabled(Marker marker) {
            return DEFAULT_LEVELS.indexOf("trace")
                    <= DEFAULT_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }

        @Override
        public boolean isDebugEnabled() {
            return DEFAULT_LEVELS.indexOf("debug")
                    <= DEFAULT_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }

        @Override
        public boolean isDebugEnabled(Marker marker) {
            return DEFAULT_LEVELS.indexOf("debug")
                    <= DEFAULT_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }

        @Override
        public boolean isInfoEnabled() {
            return DEFAULT_LEVELS.indexOf("info")
                    <= DEFAULT_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }

        @Override
        public boolean isInfoEnabled(Marker marker) {
            return DEFAULT_LEVELS.indexOf("info")
                    <= DEFAULT_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }

        @Override
        public boolean isWarnEnabled() {
            return DEFAULT_LEVELS.indexOf("warn")
                    <= DEFAULT_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }

        @Override
        public boolean isWarnEnabled(Marker marker) {
            return DEFAULT_LEVELS.indexOf("warn")
                    <= DEFAULT_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }

        @Override
        public boolean isErrorEnabled() {
            return DEFAULT_LEVELS.indexOf("error")
                    <= DEFAULT_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }

        @Override
        public boolean isErrorEnabled(Marker marker) {
            return DEFAULT_LEVELS.indexOf("error")
                    <= DEFAULT_LEVELS.indexOf(AppLogs.get().getLogLevel());
        }
    }
}
