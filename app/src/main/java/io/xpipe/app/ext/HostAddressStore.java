package io.xpipe.app.ext;

import java.util.Optional;

public interface HostAddressStore extends DataStore {

    HostAddress getHostAddress();
}
