#!/bin/sh

which sdk
if [ $? -ne 0 ]; then
    curl -s "https://get.sdkman.io" | bash
    if [ $? -ne 0 ]; then
        die "sdkman failed"
    fi;
    . "$HOME/.sdkman/bin/sdkman-init.sh"
fi;

sdk install java 20.0.1-graalce
sdk default java 20.0.1-graalce
