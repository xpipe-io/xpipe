module io.xpipe.cli.test {
    exports io.xpipe.cli.test;

    opens io.xpipe.cli.test;

    requires static lombok;
    requires io.xpipe.extension;
    requires info.picocli;
    requires io.xpipe.cli;
    requires io.xpipe.beacon;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;

    // Required runtime modules
    requires jdk.charsets;
}
