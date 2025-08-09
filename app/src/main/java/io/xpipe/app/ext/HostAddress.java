package io.xpipe.app.ext;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;

@EqualsAndHashCode
public class HostAddress {

    public static HostAddress empty() {
        return new HostAddress("unknown", List.of("unknown"));
    }

    public static HostAddress of(String host) {
        if (host == null) {
            return null;
        }

        return new HostAddress(host.strip(), List.of(host));
    }

    public static HostAddress of(String host, @NonNull List<String> addresses) {
        if (host == null) {
            return null;
        }

        return new HostAddress(host.strip(), addresses.stream().map(s -> s.strip()).toList());
    }

    private final String value;
    @Getter
    private final List<String> available;

    private HostAddress(String value, List<String> available) {this.value = value;
        this.available = available;
    }

    public HostAddress withValue(String value) {
        if (value == null || !available.contains(value)) {
            return this;
        }

        return new HostAddress(value, this.available);
    }

    public boolean isSingle() {
        return available.size() == 1;
    }

    @Override
    public String toString() {
        return value;
    }

    public String get() {
        return value;
    }

}
