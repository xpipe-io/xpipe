package io.xpipe.app.ext;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

@EqualsAndHashCode
public class HostAddress {

    public static HostAddress of(@NonNull String host) {
        return new HostAddress(host.trim());
    }

    private final String value;

    private HostAddress(String value) {this.value = value;}

    @Override
    public String toString() {
        return value;
    }

    public String get() {
        return value;
    }
}
