[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.xpipe/xpipe-beacon/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.xpipe/xpipe-beacon)
[![javadoc](https://javadoc.io/badge2/io.xpipe/xpipe-beacon/javadoc.svg)](https://javadoc.io/doc/io.xpipe/xpipe-beacon)

## X-Pipe Beacon

The X-Pipe beacon component is responsible for handling all communications between the X-Pipe daemon
and the various programming language APIs and the CLI. It provides an API that supports all kinds
of different operations.

### Inner Workings

- The underlying inter-process communication is realized through
  TCP sockets on port `21721` on Windows and `21722` on Linux.

- The data structures and exchange protocols are specified in the
  [io.xpipe.beacon.exchange package](src/main/java/io/xpipe/beacon/exchange).

- Every exchange is initiated from the outside by sending a request message to the X-Pipe daemon.
  The daemon then always sends a response message.

- The header information of a message is formatted in the json format.
  As a result, all data structures exchanged must be serializable/deserializable with jackson.

- Both the requests and responses can optionally include content in a body.
  A body is initiated with two new lines (`\n`).

- The body is split into segments of max length `65536`.
  Each segment is preceded by four bytes that specify the length of the next segment.
  In case the next segment has a length of less than `65536` bytes, we know that the end of the body has been reached.
  This way the socket communication can handle payloads of unknown length.

## Configuration

#### Custom port

The default port can be changed by passing the property `io.xpipe.beacon.port=<port>` to both the daemon and APIs.
Note that if both sides do not have the same port setting, they won't be able to reach each other.

#### Custom launch command

The beacon API also supports launching the daemon automatically in case it is not started yet.
By default, it launches the daemon of the local X-Pipe installation.
It is possible to pass a custom launch command with the property `io.xpipe.beacon.customDaemonCommand=<cmd>`
and pass arguments to it using the property `io.xpipe.beacon.daemonArgs=<args>`.
This allows for a custom launch behaviour in a testing/development environment.
Note that the `<cmd>` value has to be a single property string, which can be prone to formatting errors

#### Verbose output

By passing the property `io.xpipe.beacon.printMessages=true`, it is possible to print debug information
about the underlying communications.
In case the `io.xpipe.beacon.printDaemonOutput` property is set, the output of the daemon can also be
printed by passing the property `io.xpipe.beacon.debugExecOutput=true`.

#### Daemon debug mode

In case the daemon is started by the beacon, it is possible to customize in which mode the daemon will start up.
By passing the property `io.xpipe.beacon.launchDebugDaemon=true`, the daemon is started in debug mode,
i.e. will log more information and enable a few other options.
By passing the property `io.xpipe.beacon.attachDebuggerToDaemon=true`, it is possible to launch a daemon
in a mode where it is waiting to attach to a debugger first prior to starting up.



