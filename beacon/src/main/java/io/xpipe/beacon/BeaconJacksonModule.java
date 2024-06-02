package io.xpipe.beacon;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class BeaconJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        context.registerSubtypes(
                new NamedType(BeaconClientInformation.ApiClientInformation.class),
                new NamedType(BeaconClientInformation.CliClientInformation.class),
                new NamedType(BeaconClientInformation.DaemonInformation.class));
    }
}
