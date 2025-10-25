package io.xpipe.app.ext;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;

@EqualsAndHashCode
public class HostAddress {

    private final String value;

    @Getter
    private final List<String> available;

    private HostAddress(String value, List<String> available) {
        this.value = value;
        this.available = available;
    }

    public static HostAddress empty() {
        return new HostAddress("unknown", List.of("unknown"));
    }

    public static HostAddress of(String host) {
        if (host == null) {
            return null;
        }

        return new HostAddress(host.strip(), List.of(host));
    }

    public static HostAddress of(@NonNull List<String> addresses) {
        return new HostAddress(
                addresses.getFirst().strip(),
                addresses.stream().map(s -> s.strip()).toList());
    }

    public static HostAddress of(String host, @NonNull List<String> addresses) {
        if (host == null) {
            return null;
        }

        return new HostAddress(
                host.strip(), addresses.stream().map(s -> s.strip()).toList());
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

    public int getSelectedIndex() {
        return available.indexOf(value);
    }

    public HostAddress mergeWithIndex(HostAddress newer) {
        var index = getSelectedIndex();
        if (index < newer.getAvailable().size()) {
            return new HostAddress(newer.getAvailable().get(index), newer.getAvailable());
        } else {
            return newer;
        }
    }
}
