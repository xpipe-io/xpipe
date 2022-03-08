## X-Pipe Beacon

The X-Pipe beacon component is responsible for handling all communications between the X-Pipe daemon
and the various programming language APIs and the CLI. It provides an API that supports all kinds
of different operations.
The underlying inter-process communication is realized through TCP sockets on port `21721`.

The data structures and exchange protocols are specified in the `io.xpipe.beacon.exchange` package.
Every exchange is initiated from the outside by sending a request message to the daemon.
The daemon then always sends a response message.

The header information of a message is formatted in the json format.
As a result, all data structures exchanged must be serializable/deserializable with jackson.

Both the requests and responses can optionally include content in a body.
A body is initiated with two new lines (`\n`).
The body is split into segments of max length `65536`.
Each segment is preceded by four bytes that specify the length of the next segment.
In case the next segment has a length of less than `65536` bytes, we know that the end of the body has been reached.
This way the socket communication can handle payloads of unknown length.

### Configuration

The default port used by the beacon implementation of the X-Pipe daemon and APIs is `21721`.
It can be changed by passing the property `io.xpipe.beacon.port=<port>` to both the daemon and APIs.

The beacon API also supports launching the daemon automatically in case it is not started yet.
By default, it launches the daemon of the local X-Pipe installation.
It is possible to pass a custom launch command with the property `io.xpipe.beacon.exec=<cmd>`.
This allows for a custom launch behaviour in a testing/development environment.

By passing the property `io.xpipe.beacon.debugOutput=true`, it is possible to print debug information
about the underlying communications.
In case the `io.xpipe.beacon.exec` property is set, the output of the custom exec command can also be
printed by passing the property `io.xpipe.beacon.debugExecOutput=true`.

