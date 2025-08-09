package io.xpipe.app.update;

import io.xpipe.app.core.AppProperties;
import io.xpipe.core.OsType;

import lombok.Value;

@Value
public class AppRelease {

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
                .formatted(AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe", tag, name);
        var browser = "https://github.com/xpipe-io/%s/releases/%s"
                .formatted(AppProperties.get().isStaging() ? "xpipe-ptb" : "xpipe", tag);
        return new AppRelease(tag, url, browser, name);
    }

    String tag;
    String url;
    String browserUrl;
    String file;
}
