package io.xpipe.beacon;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class BeaconJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        context.registerSubtypes(
                new NamedType(BeaconClient.ApiClientInformation.class),
                new NamedType(BeaconClient.CliClientInformation.class),
                new NamedType(BeaconClient.ReachableCheckInformation.class)
        );
    }
}
