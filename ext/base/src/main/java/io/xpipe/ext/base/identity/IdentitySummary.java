package io.xpipe.ext.base.identity;

import io.xpipe.app.secret.SecretNoneStrategy;
import io.xpipe.ext.base.identity.ssh.NoIdentityStrategy;

public class IdentitySummary {

    public static String createSummary(IdentityStore st) {
        var user = st.getUsername().hasUser()
                ? st.getUsername().getFixedUsername().map(s -> "User " + s).orElse("User")
                : "Anonymous User";
        var s = user
                + (st.getPassword() == null || st.getPassword() instanceof SecretNoneStrategy ? "" : " + Password")
                + (st.getSshIdentity() == null || st.getSshIdentity() instanceof NoIdentityStrategy ? "" : " + Key");
        return s;
    }
}
