package io.xpipe.beacon;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
public interface BeaconAuthMethod {

    @JsonTypeName("Local")
    @Value
    @Builder
    @Jacksonized
    public static class Local implements BeaconAuthMethod {

        @NonNull
        String authFileContent;
    }

    @JsonTypeName("ApiKey")
    @Value
    @Builder
    @Jacksonized
    public static class ApiKey implements BeaconAuthMethod {

        @NonNull
        String key;
    }
}
