package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.ApplicationPathAction;

public interface JavaAction extends ApplicationPathAction {

    @Override
    default String getExecutable() {
        return "java";
    }
}
