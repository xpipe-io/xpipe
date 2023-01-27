open module io.xpipe.app.test {
    requires io.xpipe.app;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;
    requires io.xpipe.core;
    requires io.xpipe.api;
    requires io.xpipe.extension;
    requires static lombok;

    exports test;
}
