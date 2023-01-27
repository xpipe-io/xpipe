module io.xpipe.csv.test {
    exports io.xpipe.ext.csv.test;

    opens io.xpipe.ext.csv.test;

    requires io.xpipe.ext.csv;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;
    requires io.xpipe.core;
    requires io.xpipe.extension;
    requires io.xpipe.api;
}
