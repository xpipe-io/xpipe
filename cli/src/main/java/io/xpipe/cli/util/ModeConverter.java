package io.xpipe.cli.util;

import io.xpipe.core.util.XPipeDaemonMode;
import picocli.CommandLine;

public class ModeConverter implements CommandLine.ITypeConverter<XPipeDaemonMode> {

    @Override
    public XPipeDaemonMode convert(String value) throws Exception {
        return XPipeDaemonMode.get(value);
    }
}
