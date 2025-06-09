package io.xpipe.app.browser.action;

import io.xpipe.app.action.ActionProvider;

public class BrowserActionProviders {

    public static BrowserActionProvider forClass(Class<? extends BrowserActionProvider> clazz) {
        return (BrowserActionProvider) ActionProvider.ALL.stream()
                .filter(actionProvider -> actionProvider.getClass().equals(clazz))
                .findFirst()
                .orElseThrow();
    }
}
