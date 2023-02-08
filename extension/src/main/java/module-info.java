import io.xpipe.core.util.ProxyFunction;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.DataStoreActionProvider;
import io.xpipe.extension.DataSourceTarget;
import io.xpipe.extension.prefs.PrefsProvider;
import io.xpipe.extension.util.ActionProvider;
import io.xpipe.extension.util.ModuleLayerLoader;
import io.xpipe.extension.util.XPipeDaemon;

open module io.xpipe.extension {
    exports io.xpipe.extension;
    exports io.xpipe.extension.event;
    exports io.xpipe.extension.prefs;
    exports io.xpipe.extension.util;
    exports io.xpipe.extension.fxcomps;
    exports io.xpipe.extension.fxcomps.impl;
    exports io.xpipe.extension.fxcomps.util;
    exports io.xpipe.extension.fxcomps.augment;
    exports io.xpipe.extension.test;

    requires transitive io.xpipe.core;
    requires io.xpipe.beacon;
    requires io.xpipe.api;
    requires static org.hamcrest;
    requires com.fasterxml.jackson.databind;
    requires static com.sun.jna;
    requires static com.sun.jna.platform;
    requires static org.junit.jupiter.api;
    requires static org.junit.jupiter.params;
    requires static org.apache.commons.lang3;
    requires static javafx.base;
    requires static javafx.graphics;
    requires static javafx.controls;
    requires static javafx.web;
    requires static lombok;
    requires static org.controlsfx.controls;
    requires static java.desktop;
    requires static org.fxmisc.richtext;
    requires static net.synedra.validatorfx;
    requires static org.fxmisc.flowless;
    requires static org.kordamp.ikonli.javafx;
    requires static com.jfoenix;
    requires static com.dlsc.preferencesfx;
    requires static com.dlsc.formsfx;

    uses DataSourceProvider;
    uses DataSourceTarget;
    uses DataStoreActionProvider;
    uses io.xpipe.extension.I18n;
    uses io.xpipe.extension.event.EventHandler;
    uses io.xpipe.extension.prefs.PrefsProvider;
    uses io.xpipe.extension.DataStoreProvider;
    uses XPipeDaemon;
    uses io.xpipe.extension.Cache;
    uses ProxyFunction;
    uses ActionProvider;
    uses io.xpipe.extension.util.ModuleLayerLoader;

    provides ModuleLayerLoader with DataSourceTarget.Loader, ActionProvider.Loader, PrefsProvider.Loader;
}
