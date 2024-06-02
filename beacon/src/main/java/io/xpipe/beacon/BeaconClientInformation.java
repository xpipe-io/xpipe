package io.xpipe.beacon;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
public abstract class BeaconClientInformation {

    public abstract String toDisplayString();

    @JsonTypeName("cli")
    @Value
    @Builder
    @Jacksonized
    @EqualsAndHashCode(callSuper = false)
    public static class CliClientInformation extends BeaconClientInformation {

        @Override
        public String toDisplayString() {
            return "XPipe CLI";
        }
    }

    @JsonTypeName("daemon")
    @Value
    @Builder
    @Jacksonized
    @EqualsAndHashCode(callSuper = false)
    public static class DaemonInformation extends BeaconClientInformation {

        @Override
        public String toDisplayString() {
            return "Daemon";
        }
    }

    @JsonTypeName("api")
    @Value
    @Builder
    @Jacksonized
    @EqualsAndHashCode(callSuper = false)
    public static class ApiClientInformation extends BeaconClientInformation {

        String name;

        @Override
        public String toDisplayString() {
            return name;
        }
    }
}
