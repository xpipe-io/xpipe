package io.xpipe.ext.base.service;

import io.xpipe.app.util.Validators;
import io.xpipe.core.store.NetworkTunnelStore;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@SuperBuilder
@Jacksonized
@JsonTypeName("customServiceGroup")
public class CustomServiceGroupStore extends AbstractServiceGroupStore<NetworkTunnelStore> {

    @Override
    public void checkComplete() throws Throwable {
        super.checkComplete();
        Validators.isType(getParent(), NetworkTunnelStore.class);
    }
}
