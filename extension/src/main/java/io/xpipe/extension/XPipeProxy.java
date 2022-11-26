package io.xpipe.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.xpipe.api.connector.XPipeConnection;
import io.xpipe.beacon.exchange.ProxyReadConnectionExchange;
import io.xpipe.core.impl.InputStreamStore;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceReadConnection;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.XPipeInstallation;
import io.xpipe.extension.util.XPipeDaemon;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

public class XPipeProxy {

    @SneakyThrows
    private  static DataSource<?> downstreamTransform(DataSource<?> input, ShellStore proxy) {
        var proxyNode = JacksonMapper.newMapper().valueToTree(proxy);
        var inputNode = JacksonMapper.newMapper().valueToTree(input);
        var localNode = JacksonMapper.newMapper().valueToTree(ShellStore.local());
        replace(inputNode, node -> node.equals(proxyNode) ? Optional.of(localNode) : Optional.empty());
        return JacksonMapper.newMapper().treeToValue(inputNode, DataSource.class);
    }

    private   static JsonNode replace(
            JsonNode node, Function<JsonNode, Optional<JsonNode>> function) {
        var value = function.apply(node);
        if (value.isPresent()) {
            return value.get();
        }

        if (!node.isObject()) {
            return node;
        }

        var replacement = JsonNodeFactory.instance.objectNode();
        var iterator = node.fields();
        while (iterator.hasNext()) {
            var stringJsonNodeEntry = iterator.next();
            var resolved = function.apply(stringJsonNodeEntry.getValue()).orElse(stringJsonNodeEntry.getValue());
            replacement.set(stringJsonNodeEntry.getKey(), resolved);
        }
        return replacement;
    }

    public static <T extends DataSourceReadConnection> T remoteReadConnection(DataSource<?> source, ShellStore proxy) {
        var downstream = downstreamTransform(source, proxy);
        return (T) XPipeConnection.execute(con -> {
            con.sendRequest(ProxyReadConnectionExchange.Request.builder().source(downstream).build());
            con.receiveResponse();
            var inputSource = DataSource.createInternalDataSource(
                    source.determineInfo().getType(), new InputStreamStore(con.receiveBody()));
            return inputSource.openReadConnection();
        });
    }

    public static ShellStore getProxy(Object base) {
        return base instanceof Proxyable p ? p.getProxy() : null;
    }

    public static boolean isRemote(Object base) {
        return base instanceof Proxyable p && !ShellStore.isLocal(p.getProxy());
    }

    public static void checkSupport(ShellStore store) throws Exception {
        var version = XPipeDaemon.getInstance().getVersion();
        try (ShellProcessControl s = store.create().start()) {
            var installationVersion = XPipeInstallation.queryInstallationVersion(s);
            if (installationVersion.isEmpty()) {
                throw new IOException(I18n.get("noInstallationFound"));
            }

            if (!version.equals(installationVersion.get())) {
                throw new IOException(I18n.get("versionMismatch", version, installationVersion));
            }
        }
    }
}
