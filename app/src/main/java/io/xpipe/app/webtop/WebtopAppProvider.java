package io.xpipe.app.webtop;

public interface WebtopAppProvider {

    default WebtopApp getRequiredWebtopApp() {
        return null;
    }
}
