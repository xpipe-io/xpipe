package io.xpipe.ext.base.service;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Jacksonized
@JsonTypeName("mappedService")
public class MappedServiceStore extends FixedServiceStore {

    private final int containerPort;

    @Override
    public boolean licenseRequired() {
        return true;
    }
}
