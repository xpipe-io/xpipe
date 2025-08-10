package io.xpipe.app.update;

import io.xpipe.app.core.AppNames;
import io.xpipe.app.core.AppProperties;
import io.xpipe.core.OsType;

import lombok.Value;

@Value
public class AppRelease {

    String tag;
    String url;
    String browserUrl;
    String file;

    public static AppRelease of(String tag) {
        var type = AppInstaller.getSuitablePlatformAsset();
        var os =
                switch (OsType.getLocal()) {
                    case OsType.Linux ignored -> "linux";
                    case OsType.MacOs ignored -> "macos";
                    case OsType.Windows ignored -> "windows";
                };
        var arch = AppProperties.get().getArch();
        var name = "xpipe-installer-%s-%s.%s".formatted(os, arch, type.getExtension());
        var url = "https://github.com/xpipe-io/%s/releases/download/%s/%s"
                .formatted(AppNames.ofCurrent().getKebapName(), tag, name);
        var browser = "https://github.com/xpipe-io/%s/releases/%s"
                .formatted(AppNames.ofCurrent().getKebapName(), tag);
        return new AppRelease(tag, url, browser, name);
    }
}
