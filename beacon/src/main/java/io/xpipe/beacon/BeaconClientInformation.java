package io.xpipe.beacon;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class BeaconClientInformation {

    public abstract String toDisplayString();

    @JsonTypeName("Cli")
    @Value
    @Builder
    @Jacksonized
    @EqualsAndHashCode(callSuper = false)
    public static class Cli extends BeaconClientInformation {

        @Override
        public String toDisplayString() {
            return "XPipe CLI";
        }
    }

    @JsonTypeName("Daemon")
    @Value
    @Builder
    @Jacksonized
    @EqualsAndHashCode(callSuper = false)
    public static class Daemon extends BeaconClientInformation {

        @Override
        public String toDisplayString() {
            return "Daemon";
        }
    }

    @JsonTypeName("Api")
    @Value
    @Builder
    @Jacksonized
    @EqualsAndHashCode(callSuper = false)
    public static class Api extends BeaconClientInformation {

        @NonNull
        String name;

        @Override
        public String toDisplayString() {
            return name;
        }
    }
}
