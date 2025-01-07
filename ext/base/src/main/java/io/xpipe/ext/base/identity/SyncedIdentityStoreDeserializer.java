package io.xpipe.ext.base.identity;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.xpipe.app.storage.DataStorageSecret;
import io.xpipe.app.util.EncryptedValue;
import io.xpipe.app.util.SecretRetrievalStrategy;
import io.xpipe.core.util.JacksonExtension;
import io.xpipe.core.util.JacksonMapper;

import java.io.IOException;

public class SyncedIdentityStoreDeserializer extends JsonDeserializer<SyncedIdentityStore> implements JacksonExtension  {

    @Override
    public SyncedIdentityStore deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode tree = JacksonMapper.getDefault().readTree(p);
        var perUser = tree.get("perUser");
        if (perUser == null) {
            return null;
        }

        var user = tree.required("username");
        EncryptedValue<SecretRetrievalStrategy> pass = JacksonMapper.getDefault().treeToValue(tree.required("password"), new TypeReference<EncryptedValue<SecretRetrievalStrategy>>() {});
        EncryptedValue<SshIdentityStrategy> ssh = JacksonMapper.getDefault().treeToValue(tree.required("sshIdentity"), new TypeReference<EncryptedValue<SshIdentityStrategy>>() {});
        if (pass == null || ssh == null || user == null) {
            return null;
        }

        var pu = perUser.asBoolean();
        return SyncedIdentityStore.builder().username(user.asText()).perUser(pu).password(pu ? new EncryptedValue.CurrentKey<>(pass.getValue(), pass.getSecret()) :
                new EncryptedValue.VaultKey<>(pass.getValue(), pass.getSecret()))
                .sshIdentity(pu ? new EncryptedValue.CurrentKey<>(ssh.getValue(), ssh.getSecret()) :
                new EncryptedValue.VaultKey<>(ssh.getValue(), ssh.getSecret())).build();
    }

    @Override
    public Class<?> getType() {
        return SyncedIdentityStore.class;
    }
}
