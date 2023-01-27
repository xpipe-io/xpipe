package io.xpipe.cli;

import io.xpipe.beacon.exchange.MessageExchanges;
import io.xpipe.cli.util.CliProperties;
import io.xpipe.cli.util.PrettyTimeHelper;
import io.xpipe.core.impl.LocalProcessControlProvider;
import io.xpipe.core.util.JacksonMapper;

public class BuildTimeInitialization {

    static {
        // System.out.println("Starting build time initialization");
        CliProperties.init();
        JacksonMapper.initClassBased();
        MessageExchanges.loadAll();
        LocalProcessControlProvider.init(null);
        PrettyTimeHelper.init();
        // System.out.println("Ending build time initialization");
    }
}
