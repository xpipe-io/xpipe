#!/bin/bash

which sdk
if [ $? -ne 0 ]; then
    curl -s "https://get.sdkman.io" | bash
    if [ $? -ne 0 ]; then
        echo "sdkman failed"
        exit 1
    fi;
    . "$HOME/.sdkman/bin/sdkman-init.sh"
fi;

sdk install java 21.0.2-graalce
sdk default java 21.0.2-graalce
