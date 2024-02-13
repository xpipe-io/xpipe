open module io.xpipe.ext.base.test {
    requires io.xpipe.ext.base;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;
    requires io.xpipe.core;
    requires static lombok;
    requires io.xpipe.app;

    exports test;
}
