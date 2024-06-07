package io.xpipe.ext.base.service;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder
@Getter
@Jacksonized
@JsonTypeName("mappedService")
public class MappedServiceStore extends FixedServiceStore {

    private final int containerPort;
}
