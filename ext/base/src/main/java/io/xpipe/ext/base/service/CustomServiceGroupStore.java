package io.xpipe.ext.base.service;

import io.xpipe.app.util.Validators;
import io.xpipe.core.store.NetworkTunnelStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@SuperBuilder
@Jacksonized
@JsonTypeName("customServiceGroup")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CustomServiceGroupStore extends AbstractServiceGroupStore<NetworkTunnelStore> {

    @Override
    public void checkComplete() throws Throwable {
        super.checkComplete();
        Validators.isType(getParent(), NetworkTunnelStore.class);
    }
}
