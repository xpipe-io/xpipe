package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BrowserApplicationPathAction;

public interface JavaAction extends BrowserApplicationPathAction {

    @Override
    default String getExecutable() {
        return "java";
    }
}
