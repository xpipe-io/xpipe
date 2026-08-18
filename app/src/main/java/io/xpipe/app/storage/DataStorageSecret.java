package io.xpipe.app.storage;

import io.xpipe.app.secret.*;
import io.xpipe.app.util.AesSecretValue;
import io.xpipe.app.util.JacksonMapper;
import io.xpipe.app.util.SecretValue;

import lombok.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.stream.Collectors;

public class DataStorageSecret {

    public static boolean matches(JsonNode node) {
        return node.isObject() && node.has("secrets") && node.size() == 1;
    }

    @Value
    private static class Entry {

        EncryptionPrincipal principal;
        String encrypted;
        int iteration;
        EncryptionToken token;
    }

    private final InPlaceSecretValue secret;

    private final List<Entry> entries;

    private DataStorageSecret(List<Entry> entries, InPlaceSecretValue secret) {
        this.entries = entries;
        this.secret = secret;
    }

    public Set<EncryptionPrincipal> getEncryptionPrincipals() {
        return entries.stream().map(entry -> entry.getPrincipal()).collect(Collectors.toSet());
    }

    public DataStoreAccessScope getScope() {
        return DataStoreAccessScope.of(getEncryptionPrincipals());
    }

    public boolean isAccessible() {
        return secret != null;
    }

    public boolean matchesScope(DataStoreAccessScope scope) {
        var hasAll = scope.getPrincipals().stream().allMatch(encryptionPrincipal -> entries.stream()
                .anyMatch(entry -> entry.getPrincipal().equals(encryptionPrincipal)));
        return hasAll && entries.size() == scope.getPrincipals().size();
    }

    public static DataStorageSecret deserialize(JsonNode tree) {
        if (!tree.isObject()) {
            return null;
        }

        var obj = (ObjectNode) tree;
        var secretTree = obj.get("secrets");
        if (secretTree == null || !secretTree.isArray()) {
            return null;
        }

        InPlaceSecretValue maxAccessibleSecret = null;
        int maxAccessibleIteration = 0;

        var entries = new ArrayList<Entry>();
        var handler = DataStorageAccessHandler.getInstance();
        for (JsonNode jsonNode : secretTree) {
            var secretNode = jsonNode.get("secret");
            var uuidNode = jsonNode.get("principal");
            var iterationNode = jsonNode.get("iteration");
            var tokenNode = jsonNode.get("token");
            if (secretNode == null || uuidNode == null || iterationNode == null || tokenNode == null) {
                continue;
            }

            var uuid = JacksonMapper.getDefault().treeToValue(uuidNode, UUID.class);
            var iteration = iterationNode.intValue();
            var token = JacksonMapper.getDefault().treeToValue(tokenNode, EncryptionToken.class);

            var p = handler.getEncryptionPrincipal(uuid);
            if (p.isEmpty()) {
                continue;
            }

            var entry = new Entry(p.get(), secretNode.stringValue(), iteration, token);
            if (!p.get().isAccessible()) {
                entries.add(entry);
            } else {
                var tokenMatches = token.matches(p.get());
                if (tokenMatches) {
                    entries.add(entry);
                    var secret = PrincipalSecretValue.builder()
                            .principal(entry.getPrincipal().getUuid())
                            .encryptedValue(entry.getEncrypted())
                            .build();
                    var secretValid = secret.getSecret().length != 0;
                    if (secretValid) {
                        if (iteration > maxAccessibleIteration) {
                            maxAccessibleIteration = iteration;
                            maxAccessibleSecret = secret.inPlace();
                        }
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            return null;
        }

        return new DataStorageSecret(entries, maxAccessibleSecret);
    }

    public static DataStorageSecret of(SecretValue internalSecret, Set<EncryptionPrincipal> principals) {
        for (EncryptionPrincipal principal : principals) {
            if (!principal.isAccessible()) {
                throw new IllegalArgumentException("Principal " + principal.getName() + " is not accessible");
            }
        }

        var l = new ArrayList<Entry>();
        for (EncryptionPrincipal principal : principals) {
            var enc = AesSecretValue.encrypt(internalSecret.getSecret(), principal.getSecretKey());
            l.add(new Entry(principal, enc.getEncryptedValue(), 1, EncryptionToken.of(principal)));
        }
        return new DataStorageSecret(l, internalSecret.inPlace());
    }

    public DataStorageSecret with(InPlaceSecretValue secret, DataStoreAccessScope scope) {
        var l = new ArrayList<Entry>();
        for (EncryptionPrincipal principal : scope.getPrincipals()) {
            var enc = AesSecretValue.encrypt(secret.getSecret(), principal.getSecretKey());
            var existingEntry = entries.stream()
                    .filter(entry -> entry.getPrincipal().equals(principal))
                    .findFirst();
            if (existingEntry.isPresent()) {
                l.add(new Entry(
                        principal,
                        enc.getEncryptedValue(),
                        existingEntry.get().getIteration() + 1,
                        existingEntry.get().getToken()));
            } else {
                l.add(new Entry(principal, enc.getEncryptedValue(), 1, EncryptionToken.of(principal)));
            }
        }

        for (var e : entries) {
            if (scope.getPrincipals().contains(e.getPrincipal())) {
                continue;
            }

            l.add(e);
        }

        return new DataStorageSecret(l, secret);
    }

    public JsonNode serialize() {
        var ar = JsonNodeFactory.instance.arrayNode();
        for (var e : entries) {
            var targetPrincipal = EncryptionPrincipal.getTargetPrincipal(e.getPrincipal());
            var changedPrincipal = !targetPrincipal.equals(e.getPrincipal());
            var token = changedPrincipal ? EncryptionToken.of(targetPrincipal) : e.getToken();
            var secret = changedPrincipal
                    ? AesSecretValue.encrypt(getInternalSecret().getSecret(), targetPrincipal.getSecretKey())
                            .getEncryptedValue()
                    : e.getEncrypted();

            var node = JsonNodeFactory.instance.objectNode();
            node.put("name", targetPrincipal.getName());
            node.put("principal", targetPrincipal.getUuid().toString());
            node.put("iteration", e.getIteration());
            node.put("secret", secret);
            node.set("token", JacksonMapper.getDefault().valueToTree(token));

            ar.add(node);
        }

        var secretsNode = JsonNodeFactory.instance.objectNode();
        secretsNode.set("secrets", ar);
        return secretsNode;
    }

    public DataStorageSecret withUpdatedPrincipals() {
        var newEntries = new ArrayList<Entry>();
        for (var e : entries) {
            Entry newEntry;
            var targetPrincipal = EncryptionPrincipal.getTargetPrincipal(e.getPrincipal());
            var updatePrincipal = isAccessible()
                    && targetPrincipal.isAccessible()
                    && (!targetPrincipal.equals(e.getPrincipal())
                            || !e.getToken().matches(e.getPrincipal()));
            if (updatePrincipal) {
                var enc = AesSecretValue.encrypt(secret.getSecret(), targetPrincipal.getSecretKey());
                var encToken = EncryptionToken.of(targetPrincipal);
                newEntry = new Entry(targetPrincipal, enc.getEncryptedValue(), e.getIteration(), encToken);
            } else {
                newEntry = e;
            }
            newEntries.add(newEntry);
        }

        if (newEntries.equals(entries)) {
            return this;
        }

        return new DataStorageSecret(newEntries, secret);
    }

    public InPlaceSecretValue getInternalSecret() {
        return secret;
    }
}
