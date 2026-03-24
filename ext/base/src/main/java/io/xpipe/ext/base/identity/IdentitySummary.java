package io.xpipe.ext.base.identity;

import io.xpipe.app.cred.NoIdentityStrategy;
import io.xpipe.app.secret.SecretNoneStrategy;

public class IdentitySummary {

    public static String createSummary(IdentityStore st) {
        if (st instanceof MultiIdentityStore mis) {
            var selected = mis.getSelected();
            if (selected.isPresent()) {
                return createSummary(selected.get().getStore()) + " ["
                        + selected.get().get().getName() + "]";
            }
        }

        if (st instanceof PasswordManagerIdentityStore pmis) {
            var s = "Credentials " + pmis.getKey();
            if (pmis.hasAgentKey()) {
                s += " + agent key";
            }
            return s;
        }

        var user = st.getUsername().hasUser()
                ? st.getUsername().getFixedUsername().map(s -> "User " + s).orElse("User")
                : "Anonymous user";
        var s = user
                + (st.getPassword() == null || st.getPassword() instanceof SecretNoneStrategy ? "" : " + password")
                + (st.getSshIdentity() == null || st.getSshIdentity() instanceof NoIdentityStrategy ? "" : " + key");
        return s;
    }
}
