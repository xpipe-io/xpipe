#!/bin/bash

DIR="${0%/*}"
EXTRA_ARGS=(JVM-ARGS)
export CDS_JVM_OPTS="${EXTRA_ARGS[*]}"
unset _JAVA_OPTIONS
unset JAVA_TOOL_OPTIONS

"$DIR/../lib/runtime/bin/xpiped" "$@"

read -rsp "Press any key to close" -n 1 key
