package io.xpipe.core.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.Arrays;

@Getter
public abstract class ProxyFunction {

    private static ModuleLayer layer;

    public static void init(ModuleLayer l) {
        layer = l;
    }

    @SneakyThrows
    public ProxyFunction callAndCopy() {
        var proxyStore = ProxyProvider.get().getProxy(getProxyBase());
        if (proxyStore != null) {
            return ProxyProvider.get().call(this, proxyStore);
        } else {
            callLocal();
            return this;
        }
    }

    @SneakyThrows
    protected Object getProxyBase() {
        var first = Arrays.stream(getClass().getDeclaredFields()).findFirst().orElseThrow();
        first.setAccessible(true);
        return first.get(this);
    }

    public abstract void callLocal();

    public static class Serializer extends StdSerializer<ProxyFunction> {

        protected Serializer() {
            super(ProxyFunction.class);
        }

        @Override
        public void serialize(ProxyFunction value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            var node = (ObjectNode) JacksonMapper.getDefault().valueToTree(value);
            node.set("module", new TextNode(value.getClass().getModule().getName()));
            node.set("class", new TextNode(value.getClass().getName()));
            gen.writeTree(node);
        }
    }

    public static class Deserializer extends StdDeserializer<ProxyFunction> {

        protected Deserializer() {
            super(ProxyFunction.class);
        }

        @Override
        @SneakyThrows
        public ProxyFunction deserialize(JsonParser p, DeserializationContext ctxt) {
            var tree = (ObjectNode) JacksonMapper.getDefault().readTree(p);
            var moduleReference = tree.remove("module").asText();
            var classReference = tree.remove("class").asText();
            var module = layer.findModule(moduleReference).orElseThrow();
            var targetClass = Class.forName(module, classReference);
            if (targetClass == null) {
                throw new IllegalArgumentException("Named function class not found: " + classReference);
            }
            return (ProxyFunction) JacksonMapper.getDefault().treeToValue(tree, targetClass);
        }
    }
}
