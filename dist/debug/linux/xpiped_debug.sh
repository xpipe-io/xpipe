#!/bin/bash

DIR="${0%/*}"
EXTRA_ARGS=(JVM-ARGS)

"$DIR/../lib/runtime/bin/xpiped" "${EXTRA_ARGS[@]}" "$@"
