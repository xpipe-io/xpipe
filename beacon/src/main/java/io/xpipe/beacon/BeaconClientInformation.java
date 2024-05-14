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

    public final CliClientInformation cli() {
        return (CliClientInformation) this;
    }

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

    @JsonTypeName("gateway")
    @Value
    @Builder
    @Jacksonized
    @EqualsAndHashCode(callSuper = false)
    public static class GatewayClientInformation extends BeaconClientInformation {

        String version;

        @Override
        public String toDisplayString() {
            return "XPipe Gateway " + version;
        }
    }

    @JsonTypeName("api")
    @Value
    @Builder
    @Jacksonized
    @EqualsAndHashCode(callSuper = false)
    public static class ApiClientInformation extends BeaconClientInformation {

        String version;
        String language;

        @Override
        public String toDisplayString() {
            return String.format("XPipe %s API v%s", language, version);
        }
    }
}
