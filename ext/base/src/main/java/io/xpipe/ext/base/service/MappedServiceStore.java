package io.xpipe.ext.base.service;

import io.xpipe.app.store.FixedChildStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder(toBuilder = true)
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Jacksonized
@JsonTypeName("mappedService")
public class MappedServiceStore extends FixedServiceStore {

    int containerPort;

    @Override
    public FixedChildStore merge(FixedChildStore other) {
        var o = (MappedServiceStore) other;
        return toBuilder().tunnelToLocalhost(o.getTunnelToLocalhost()).build();
    }

    @Override
    public boolean licenseRequired() {
        return true;
    }
}
