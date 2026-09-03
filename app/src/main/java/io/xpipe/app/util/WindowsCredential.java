package io.xpipe.app.util;

import lombok.Value;

@Value
public class WindowsCredential {
    String target;
    String username;
    String password;
}
