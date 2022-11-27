package io.xpipe.beacon;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xpipe.beacon.exchange.NamedFunctionExchange;
import io.xpipe.core.util.Proxyable;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.Arrays;

@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public abstract class NamedFunction<T> {

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
