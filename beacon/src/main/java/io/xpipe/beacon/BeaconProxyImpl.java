package io.xpipe.beacon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.xpipe.beacon.exchange.ProxyFunctionExchange;
import io.xpipe.beacon.exchange.ProxyReadConnectionExchange;
import io.xpipe.beacon.exchange.ProxyWriteConnectionExchange;
import io.xpipe.core.impl.InputStreamStore;
import io.xpipe.core.impl.OutputStreamStore;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceConnection;
import io.xpipe.core.source.DataSourceReadConnection;
import io.xpipe.core.source.WriteMode;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.ProxyFunction;
import io.xpipe.core.util.ProxyProvider;
import io.xpipe.core.util.Proxyable;
import lombok.SneakyThrows;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

public class BeaconProxyImpl extends ProxyProvider {

    @SneakyThrows
    private static DataSource<?> downstreamTransform(DataSource<?> input, ShellStore proxy) {
        var proxyNode = JacksonMapper.newMapper().valueToTree(proxy);
        var inputNode = JacksonMapper.newMapper().valueToTree(input);
        var localNode = JacksonMapper.newMapper().valueToTree(ShellStore.local());
        replace(inputNode, node -> node.equals(proxyNode) ? Optional.of(localNode) : Optional.empty());
        return JacksonMapper.newMapper().treeToValue(inputNode, DataSource.class);
    }

    private static JsonNode replace(JsonNode node, Function<JsonNode, Optional<JsonNode>> function) {
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

    @Override
    public ShellStore getProxy(Object base) {
        var proxy = base instanceof Proxyable p ? p.getProxy() : null;
        return ShellStore.isLocal(proxy) ? (BeaconConfig.localProxy() ? proxy : null) : proxy;
    }

    @Override
    public boolean isRemote(Object base) {
        if (base == null) {
            throw new IllegalArgumentException("Proxy base is null");
        }

        return getProxy(base) != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends DataSourceReadConnection> T createRemoteReadConnection(DataSource<?> source, ShellStore proxy) throws Exception {
        var downstream = downstreamTransform(source, proxy);

        BeaconClient client = null;
        try {
            client = BeaconClient.connectProxy(proxy);
            client.sendRequest(ProxyReadConnectionExchange.Request.builder()
                    .source(downstream)
                    .build());
            client.receiveResponse();
            BeaconClient finalClient = client;
            var inputStream = new FilterInputStream(finalClient.receiveBody()) {
                @Override
                @SneakyThrows
                public void close() throws IOException {
                    super.close();
                    finalClient.close();
                }
            };
            var inputSource = DataSource.createInternalDataSource(source.getType(), new InputStreamStore(inputStream));
            return (T) inputSource.openReadConnection();
        } catch (Exception ex) {
            if (client != null) client.close();
            throw ex;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends DataSourceConnection> T createRemoteWriteConnection(DataSource<?> source, WriteMode mode,  ShellStore proxy) throws Exception {
        var downstream = downstreamTransform(source, proxy);

        BeaconClient client = null;
        try {
            client = BeaconClient.connectProxy(proxy);
            client.sendRequest(ProxyWriteConnectionExchange.Request.builder()
                    .source(downstream)
                    .build());
            BeaconClient finalClient = client;
            var outputStream = new FilterOutputStream(client.sendBody()) {
                @Override
                @SneakyThrows
                public void close() throws IOException {
                    super.close();
                    finalClient.receiveResponse();
                    finalClient.close();
                }
            };
            var outputSource = DataSource.createInternalDataSource(source.getType(), new OutputStreamStore(outputStream));
            return (T) outputSource.openWriteConnection(mode);
        } catch (Exception ex) {
            if (client != null) client.close();
            throw ex;
        }
    }

    @Override
    @SneakyThrows
    public ProxyFunction call(ProxyFunction func, ShellStore proxy) {
        try (var client = BeaconClient.connectProxy(proxy)) {
            client.sendRequest(
                    ProxyFunctionExchange.Request.builder().function(func).build());
            ProxyFunctionExchange.Response response = client.receiveResponse();
            return response.getFunction();
        }
    }
}
