package io.xpipe.beacon;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.xpipe.beacon.exchange.NamedFunctionExchange;
import io.xpipe.core.util.JacksonMapper;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.Arrays;

@Getter
public abstract class NamedFunction<T> {

    private static ModuleLayer layer;

    public static void init(ModuleLayer l) {
        layer = l;
    }

    public static class Serializer extends StdSerializer<NamedFunction> {

        protected Serializer() {
            super(NamedFunction.class);
        }

        @Override
        public void serialize(NamedFunction value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            var node = (ObjectNode) JacksonMapper.getDefault().valueToTree(value);
            node.set("module", new TextNode(value.getClass().getModule().getName()));
            node.set("class", new TextNode(value.getClass().getName()));
            gen.writeTree(node);
        }
    }

    public static class Deserializer extends StdDeserializer<NamedFunction<?>> {

        protected Deserializer() {
            super(NamedFunction.class);
        }

        @Override
        @SneakyThrows
        public NamedFunction<?> deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JacksonException {
            var tree = (ObjectNode) JacksonMapper.getDefault().readTree(p);
            var moduleReference = tree.remove("module").asText();
            var classReference = tree.remove("class").asText();
            var module = layer.findModule(moduleReference).orElseThrow();
            var targetClass = Class.forName(module, classReference);

            if (targetClass == null) {
                throw new IllegalArgumentException("Named function class not found: " + classReference);
            }

            return (NamedFunction<?>) JacksonMapper.getDefault().treeToValue(tree, targetClass);
        }
    }

    @SneakyThrows
    public T call() {
        var proxyStore = Proxyable.getProxy(getProxyBase());
        if (proxyStore != null) {
            var client = BeaconClient.connectProxy(proxyStore);
            client.sendRequest(
                    NamedFunctionExchange.Request.builder().function(this).build());
            NamedFunctionExchange.Response response = client.receiveResponse();
            return (T) response.getReturnValue();
        } else {
            return callLocal();
        }
    }

    @SneakyThrows
    protected Object getProxyBase() {
        var first = Arrays.stream(getClass().getDeclaredFields()).findFirst().orElseThrow();
        first.setAccessible(true);
        return first.get(this);
    }

    public abstract T callLocal();
}
