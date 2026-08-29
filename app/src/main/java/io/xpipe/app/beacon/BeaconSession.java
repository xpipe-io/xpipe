package io.xpipe.app.beacon;

import lombok.Value;

@Value
public class BeaconSession {

    BeaconClientInformation clientInformation;
    String token;
}
