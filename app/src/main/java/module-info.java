import com.fasterxml.jackson.databind.Module;
import io.xpipe.app.beacon.impl.*;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.ext.*;
import io.xpipe.app.issue.EventHandler;
import io.xpipe.app.issue.EventHandlerImpl;
import io.xpipe.app.storage.DataStateProviderImpl;
import io.xpipe.app.util.AppJacksonModule;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.ProxyManagerProviderImpl;
import io.xpipe.app.util.TerminalLauncher;
import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.util.DataStateProvider;
import io.xpipe.core.util.ModuleLayerLoader;
import io.xpipe.core.util.ProxyFunction;
import io.xpipe.core.util.ProxyManagerProvider;
import org.slf4j.spi.SLF4JServiceProvider;

open module io.xpipe.app {
    exports io.xpipe.app.beacon;
    exports io.xpipe.app.core;
    exports io.xpipe.app.util;
    exports io.xpipe.app;
    exports io.xpipe.app.issue;
    exports io.xpipe.app.comp.base;
    exports io.xpipe.app.core.mode;
    exports io.xpipe.app.prefs;
    exports io.xpipe.app.comp.store;
    exports io.xpipe.app.storage;
    exports io.xpipe.app.update;
    exports io.xpipe.app.ext;
    exports io.xpipe.app.fxcomps.impl;
    exports io.xpipe.app.fxcomps;
    exports io.xpipe.app.fxcomps.util;
    exports io.xpipe.app.fxcomps.augment;
    exports io.xpipe.app.test;
    exports io.xpipe.app.browser.action;
    exports io.xpipe.app.browser;
    exports io.xpipe.app.browser.icon;
    exports io.xpipe.app.core.check;
    exports io.xpipe.app.terminal;
    exports io.xpipe.app.browser.session;
    exports io.xpipe.app.browser.fs;
    exports io.xpipe.app.browser.file;

    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.slf4j;
    requires org.slf4j.jdk.platform.logging;
    requires atlantafx.base;
    requires com.vladsch.flexmark;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.feather;
    requires io.xpipe.modulefs;
    requires io.xpipe.core;
    requires static lombok;
    requires java.desktop;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires javafx.base;
    requires static org.junit.jupiter.api;
    requires javafx.controls;
    requires javafx.media;
    requires javafx.web;
    requires javafx.graphics;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.material;
    requires io.sentry;
    requires io.xpipe.beacon;
    requires org.kohsuke.github;
    requires info.picocli;
    requires java.instrument;
    requires java.management;
    requires jdk.management;
    requires jdk.management.agent;
    requires net.steppschuh.markdowngenerator;
    requires com.shinyhut.vernacular;

    // Required runtime modules
    requires jdk.charsets;
    requires jdk.crypto.cryptoki;
    requires jdk.crypto.ec;
    requires jdk.localedata;
    requires jdk.accessibility;
    requires org.kordamp.ikonli.material2;
    requires org.kordamp.ikonli.materialdesign2;
    requires jdk.zipfs;

    // For debugging
    requires jdk.jdwp.agent;
    requires org.kordamp.ikonli.core;
    requires jdk.httpserver;

    uses TerminalLauncher;
    uses io.xpipe.app.ext.ActionProvider;
    uses EventHandler;
    uses PrefsProvider;
    uses DataStoreProvider;
    uses ProxyFunction;
    uses ModuleLayerLoader;
    uses ScanProvider;
    uses BrowserAction;
    uses LicenseProvider;
    uses io.xpipe.app.util.LicensedFeature;
    uses io.xpipe.beacon.BeaconInterface;

    provides Module with
            AppJacksonModule;
    provides ModuleLayerLoader with
            DataStoreProviders.Loader,
            ActionProvider.Loader,
            PrefsProvider.Loader,
            BrowserAction.Loader,
            LicenseProvider.Loader,
            ScanProvider.Loader;
    provides DataStateProvider with
            DataStateProviderImpl;
    provides ProxyManagerProvider with
            ProxyManagerProviderImpl;
    provides SLF4JServiceProvider with
            AppLogs.Slf4jProvider;
    provides EventHandler with
            EventHandlerImpl;
    provides BeaconInterface with
            OpenExchangeImpl,
            FocusExchangeImpl,
            StatusExchangeImpl,
            StopExchangeImpl,
            HandshakeExchangeImpl,
            ModeExchangeImpl,
            AskpassExchangeImpl,
            TerminalWaitExchangeImpl,
            TerminalLaunchExchangeImpl,
            VersionExchangeImpl;
}
