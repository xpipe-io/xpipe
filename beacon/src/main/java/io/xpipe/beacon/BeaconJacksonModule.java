package io.xpipe.beacon;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class BeaconJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        context.registerSubtypes(
                new NamedType(BeaconClientInformation.Api.class),
                new NamedType(BeaconClientInformation.Cli.class),
                new NamedType(BeaconClientInformation.Daemon.class));
        context.registerSubtypes(
                new NamedType(BeaconAuthMethod.Local.class),
                new NamedType(BeaconAuthMethod.ApiKey.class));
    }
}
