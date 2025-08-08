[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.xpipe/xpipe-beacon/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.xpipe/xpipe-beacon)
[![javadoc](https://javadoc.io/badge2/io.xpipe/xpipe-beacon/javadoc.svg)](https://javadoc.io/doc/io.xpipe/xpipe-beacon)

## XPipe Beacon

The XPipe beacon component is responsible for handling all communications between the XPipe daemon
and the APIs and the CLI. It provides an API that supports all kinds
of different operations.

For a full documentation, see the [OpenAPI spec](https://docs.xpipe.io/api)

### Inner Workings

- The underlying communication is realized through an HTTP server on port `21721`

- The data structures and exchange protocols are specified in the
  [io.xpipe.beacon.api package](src/main/java/io/xpipe/beacon/api).

- Every exchange is initiated from the outside by sending a request message to the XPipe daemon.
  The daemon then always sends a response message.

- The body of a message is usually formatted in the json format.
  As a result, all data structures exchanged must be serializable/deserializable with jackson.

## Configuration

#### Custom port

The default port can be changed by passing the property `io.xpipe.beacon.port=<port>` to the daemon.
Note that if both sides do not have the same port setting, they won't be able to reach each other.

#### Verbose output

By passing the property `io.xpipe.beacon.printMessages=true`, it is possible to print debug information
about the underlying communications.

