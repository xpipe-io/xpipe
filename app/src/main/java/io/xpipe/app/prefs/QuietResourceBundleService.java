package io.xpipe.app.prefs;

import com.dlsc.formsfx.model.util.ResourceBundleService;
import io.xpipe.extension.I18n;

import java.util.Enumeration;
import java.util.ResourceBundle;

public class QuietResourceBundleService extends ResourceBundleService {

    public QuietResourceBundleService() {
        super(new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                return null;
            }

            @Override
            public Enumeration<String> getKeys() {
                return null;
            }
        });
    }

    @Override
    public String translate(String key) {
        var value = I18n.get(key);
        return value;
    }
}
