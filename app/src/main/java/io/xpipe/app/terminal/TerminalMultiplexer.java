package io.xpipe.app.terminal;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.util.OsType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface TerminalMultiplexer {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        l.add(TmuxTerminalMultiplexer.class);
        l.add(ZellijTerminalMultiplexer.class);
        l.add(ScreenTerminalMultiplexer.class);
        return l;
    }

    static TerminalMultiplexer determineDefault(TerminalMultiplexer existing) {
        if (!AppProperties.get().isInitialLaunch()
                || OsType.ofLocal() == OsType.WINDOWS
                || AppDistributionType.get() == AppDistributionType.WEBTOP) {
            return existing;
        }

        try {
            if (existing != null && existing.shouldSelect()) {
                return existing;
            }

            var all = List.of(new TmuxTerminalMultiplexer(), new ZellijTerminalMultiplexer());
            for (TerminalMultiplexer m : all) {
                if (m.shouldSelect()) {
                    return m;
                }
            }
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
        }

        return null;
    }

    boolean supportsSplitView();

    String getDocsLink();

    boolean shouldSelect() throws Exception;

    void checkSupported(ShellControl sc) throws Exception;

    ShellScript launchForExistingSession(ShellControl control, TerminalLaunchConfiguration config);

    ShellScript launchNewSession(ShellControl control, TerminalLaunchConfiguration config) throws Exception;
}
