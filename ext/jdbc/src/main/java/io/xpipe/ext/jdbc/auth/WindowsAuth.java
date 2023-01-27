package io.xpipe.ext.jdbc.auth;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

@Value
@JsonTypeName("windowsAuth")
public class WindowsAuth implements AuthMethod {}
