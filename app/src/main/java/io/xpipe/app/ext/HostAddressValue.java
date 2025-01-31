package io.xpipe.app.ext;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.storage.DataStoreEntryRef;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = HostAddressValue.InPlace.class),
        @JsonSubTypes.Type(value = HostAddressValue.Ref.class)
})
public interface HostAddressValue {

    static HostAddressValue staticAddress(String address) {
        return staticAddress(HostAddress.of(address));
    }

    static HostAddressValue staticAddress(HostAddress address) {
        return InPlace.builder().addresses(List.of(address)).build();
    }

    List<HostAddress> getAll();

    HostAddress getDefault();

    @Value
    @NonFinal
    @JsonTypeName("inPlace")
    @Jacksonized
    @SuperBuilder
    public static class InPlace implements HostAddressValue {

        List<HostAddress> addresses;

        @Override
        public List<HostAddress> getAll() {
            return addresses;
        }

        @Override
        public HostAddress getDefault() {
            return addresses.getFirst();
        }
    }


    @JsonTypeName("ref")
    @Value
    @Jacksonized
    @Builder
    class Ref implements HostAddressValue {

        DataStoreEntryRef<HostAddressSupplierStore> ref;

        @Override
        public List<HostAddress> getAll() {
            return ref.getStore().getAllHostAddresses();
        }

        @Override
        public HostAddress getDefault() {
            return ref.getStore().getDefaultHostAddress();
        }
    }
}
