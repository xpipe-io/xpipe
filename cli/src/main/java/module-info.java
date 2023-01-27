open module io.xpipe.cli {
    exports io.xpipe.cli;
    exports io.xpipe.cli.util;
    exports io.xpipe.cli.daemon;
    exports io.xpipe.cli.meta;
    exports io.xpipe.cli.store;
    exports io.xpipe.cli.source;

    requires org.ocpsoft.prettytime;
    requires io.xpipe.beacon;
    requires io.xpipe.core;
    requires jdk.charsets;
    requires info.picocli;
    // Only available when using GraalVM
    requires org.graalvm.sdk;
    requires com.fasterxml.jackson.databind;
    requires de.vandermeer.asciitable;
    requires de.vandermeer.ascii_utf_themes;
    requires org.jline.terminal;
    requires org.jline.terminal.jna;
    requires com.sun.jna;
}
