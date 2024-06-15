package io.xpipe.app.beacon;

import io.xpipe.beacon.BeaconClientInformation;

import lombok.Value;

@Value
public class BeaconSession {

    BeaconClientInformation clientInformation;
    String token;
}
